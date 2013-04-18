package greader.api

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.{FromConfig, RoundRobinRouter}
import akka.pattern._
import akka.util.Timeout

import scala.util.{Success, Failure, Try}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.reflect.ClassTag

import spray.http._
import spray.http.MediaTypes._
import spray.http.HttpHeaders._
import spray.http.HttpCharsets._
import spray.httpx.encoding.Gzip
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling.pimpHttpEntity

import spray.json.{JsObject}

import spray.routing.{Route, HttpService, RequestContext}
import spray.routing.directives._

import greader.{Utils, Scanner, Scan, Refresh, GetInfo}
import greader.Utils.{now, loads}
import greader.domain.{Subscriptions, Feed, Item, Link, Content}

import language.postfixOps

trait GReaderAPI extends HttpService {
  import greader.Metrics._
  import GReaderJsonProtocol._
  implicit val timeout = Timeout(30 seconds)
  val scanner: ActorRef

  val idWorker = new geekple.service.IdWorker(gmetricServer, 0L, 0L, 0L)

  val greaderRoute = {
    post {
      path("v1" / "id" / PathElement) { agent =>
        respondWithMediaType(`application/json`) {
          complete {
            val id = idWorker.get_id(agent)
            """{"id": %s, "id_str": "%s"}""".format(id, id)
          }
        }
      } ~
      path("v1" / "feeds") {
        entity(as[String]) { text =>
          val subscriptions = loads(text, classOf[Subscriptions])
          encodeResponse(Gzip) {
            respondWithMediaType(`application/json`) {
              complete {
                (scanner ? GetInfo(subscriptions)).mapTo[Subscriptions]
              }
            }
          }
        }
      } ~
      path("v1" / "subscriptions") {
        entity(as[String]) { text =>
          val subscriptions = loads(text, classOf[Subscriptions])
          encodeResponse(Gzip) {
            respondWithMediaType(`application/json`) {
              complete {
                subscribeOp()
                (scanner ? Refresh(subscriptions)).mapTo[Subscriptions]
              }
            }
          }
        }
      }
    } ~
    get {
      path("v1" / "status") {
        respondWithMediaType(`application/json`) {
          encodeResponse(Gzip) {
            complete {
              gmetricServer.toString()
            }
          }
        }
      } ~
      path("v1" / "feeds" / Rest) { url =>
        parameters('c.as[String]?) { c =>
          val maxId = c match { case Some(c) => Utils.decode(c) case _ => 0 }
          respondWithMediaType(`application/json`) {
            complete {
              readOp()
              (scanner ? Scan(url)).mapTo[Feed]
            }
          }
        }
      }
    }
  }
}
