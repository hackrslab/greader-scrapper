package greader

import greader.Utils._

import geekple.service.IdWorker
import greader.domain._
import greader.Metrics._

import akka.actor.{Actor, Props, ActorSystem}
import akka.event.Logging
import akka.routing._
import akka.pattern.{ask, pipe}

import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.duration._

import java.nio.charset.Charset
import java.io._
import java.net._
import com.sun.syndication.io._
import org.xml.sax.InputSource
import com.top10.redis._

import spray.http._
import spray.http.MediaTypes._
import spray.routing.RequestContext

import language.postfixOps
import language.implicitConversions

case class Scan(url: String, count: Int = 20, maxId: Long = 0L, sinceId: Long = 0L)
case class Refresh(subscriptions: Subscriptions)
case class Message(feed: Feed)
case class GetInfo(subscriptions: Subscriptions)

class Scanner extends Actor {
  import context.dispatcher

  val localScanner = new LocalScanner()
  val scrapper = context.actorOf(Props[Scrapper]) // .withRouter(new FromConfig()), "scrapper")

  implicit val timeout = akka.util.Timeout(10 seconds)

  def receive = {
    case GetInfo(subscriptions) =>
      get(subscriptions).andThen {
        case Success(feed) =>
        case Failure(e) =>
      } pipeTo sender
    case Refresh(subscriptions) =>
      refresh(subscriptions).andThen {
        case Success(feed) =>
        case Failure(e) =>
      } pipeTo sender
    case Scan(url, count, maxId, sinceId) =>
      scan(url, count, maxId, sinceId).andThen {
        case Success(feed) =>
        case Failure(e) =>
      } pipeTo sender
  }

  private def get(s: Subscriptions): Future[Subscriptions] = future {
    val subscriptions = localScanner.gets(s)
    subscriptions.feeds.foreach { feed =>
      if (feed.stale) {
        scrapper ! Message(feed)
      }
    }

    subscriptions
  }

  private def refresh(s: Subscriptions): Future[Subscriptions] = future {
    val subscriptions = localScanner.gets(s)
    subscriptions.feeds.foreach { feed =>
      if (feed.stale) {
        scrapper ! Message(feed)
      }
    }

    subscriptions
  }

  private def scan(url: String, count: Int, maxId: Long, sinceId: Long): Future[Feed] = future {
    val feed = localScanner.scan(url, count, maxId, sinceId)
    if (feed.stale) {
      scrapper ! Message(Feed(id = url))
    }
    feed
  }
}

class LocalScanner {
  val redis = new SingleRedis("localhost", 6379)
  val emptyFeed = Feed(id = null, stale = true)

  def isStale(url: String): Boolean = {
    val start = now
    val fid = encodeURL(url)
    redis.hget("f:h:crawled", encodeURL(url)) match {
      case Some(value) if now() - 60000 < value.toLong =>
        cacheHit()
        false
      case Some(_) =>
        cacheRenew()
        true
      case None =>
        cacheMiss()
        true
    }
  }

  def gets(s: Subscriptions) = {
    val ids = s.feeds.map(_.id)
    val keys = ids.map(encodeURL)
    val times = redis.hmget("f:h:crawled", keys).map(_ match {
      case o: Option[String] => o.getOrElse("0").toLong
    })
    val feeds = redis.hmget("f:h:feeds:meta", keys).map(_ match {
      case Some(text) => loads(text, classOf[Feed])
      case _ => emptyFeed
    })
    Subscriptions(feeds = ids.zip(feeds.zip(times)).map(_ match {
      case (id, (feed, crawled)) if (now() - 60000 < crawled) =>
        feed.copy(id = id, status = "OK", stale = false)
      case (id, (feed, _)) =>
        feed.copy(id = id, status = "PENDING", stale = true)
    }))
  }

  def scan(url: String, count: Int, maxId: Long, sinceId: Long): Feed = {
    val start = now
    val fid = encodeURL(url)
    val stale = isStale(url)
    val startKey = if (maxId <= 0) java.lang.Double.POSITIVE_INFINITY else maxId - 0.1d
    val endKey = 0d
    val keys = redis.run(redis => {
      redis.zrevrangeByScore("f:z:items:"+fid, startKey, endKey, 0, count)
    }).toArray(new Array[String](0))
    val feed = redis.hget("f:h:feeds:meta", encodeURL(url)) match {
      case Some(text) =>
        loads(text, classOf[Feed]).copy(stale = stale)
      case _ =>
        cacheMiss()
        emptyFeed
    }
    val items: Seq[Item] = keys match {
      case keys if keys.size > 0 =>
        redis.hmget("f:h:items:content", keys).filter(_ != None).map(_ match {
          case Some(value) => loads(value, classOf[Item])
        })
      case _ => Seq()
    }
    localLatency(now - start)
    feed.copy(id = url, items = items.map(_.copy(categories = Seq())))
  }
}

class Scrapper extends Actor {
  val log = Logging(context.system, this)

  import domain.Conversions._

  val redis = new SingleRedis("localhost", 6379)
  val shifter = context.actorOf(Props[Shifter])

  def condition(id: String): Boolean = {
    val key = "f:scrapper:lock:%s".format(encodeURL(id))
    if (redis.setnx(key, "") == 1) {
      redis.expire(key, 10)
      true
    } else {
      false
    }
  }

  def receive = {
    case Message(feed) =>
      if (condition(feed.id)) {
        log.info("scrapping, id: {}", feed.id)
        val start = now
        val is = new URL(feed.id).openConnection.getInputStream
        val source = new InputSource(is)
        val input = new SyndFeedInput()
        val hot = input.build(source)
        remoteHit()
        remoteLatency(now - start)
        shifter ! Message(hot.copy(id = feed.id))
      }
  }
}

class Shifter extends Actor {
  val redis = new SingleRedis("localhost", 6379)

  def receive = {
    case Message(feed) =>
      val fid = encodeURL(feed.id)
      val keys = feed.items.map(i => encode(unique(fid, i.uri)).mkString)
      val cached = redis.hmget("f:h:items:published", keys).zip(redis.hmget("f:h:items:signature", keys))
      val start = now()
      redis.exec(pipe => {
        pipe.hset("f:h:crawled", fid, "%s".format(System.currentTimeMillis()))
        pipe.hset("f:h:feeds:meta", fid, dumps(feed.copy(items = null)))
        feed.items.zip(cached).map(_ match {
          case (item, (Some(published), Some(hash))) =>
            val eid = encode(unique(fid, item.uri)).mkString
            val score = unique(published.toLong, item.uri)
            signature(item) match {
              case s if s != hash =>
                pipe.hset("f:h:items:signature", eid, signature(item))
                pipe.hset("f:h:items:content", eid, dumps(item.copy(published=published.toLong)))
                pipe.rpush("f:l:changes", dumps(Map(
                  "timestamp" -> System.currentTimeMillis(),
                  "feed" -> feed.id,
                  "item" -> item.uri,
                  "title" -> item.title,
                  "published" -> item.published,
                  "categories" -> item.categories,
                  "signature" -> s
                )))
                itemsChanged()
              case _ =>
                itemsUpToDate()
            }
          case (item, (_, _)) =>
            val eid = encode(unique(fid, item.uri)).mkString
            val score = unique(item.published, item.uri)
            pipe.hset("f:h:feeds:meta", fid, dumps(feed.copy(items = null)))
            pipe.hset("f:h:items:published", eid, item.published.toString)
            pipe.hset("f:h:items:signature", eid, signature(item))
            pipe.hset("f:h:items:content", eid, dumps(item))
            pipe.zadd("f:z:items:all", score, eid)
            pipe.zadd("f:z:items:"+fid, score, eid)
        })
      })
      cacheTime(now() - start)
  }
}

