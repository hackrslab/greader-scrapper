package greader.domain

import greader.Utils._

import java.util.Date
import java.nio.ByteBuffer

import com.sun.syndication.feed.synd.{SyndContent, SyndPerson, SyndEntry}
import scala.collection.JavaConversions._

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import language.implicitConversions

object Conversions {
  def content(values: Seq[Any], defaultValue: SyndContent): Content = {
    for (value <- values) {
      val content = value match {
        case x: SyndContent if x.getType == "html" => Content(content = sanitize(x.getValue), `type` = "text/html")
        case x: SyndContent => null
      }
      if (content != null) {
        return content
      }
    }
    if (defaultValue != null) {
      Content(content = sanitize(defaultValue.getValue), `type` = defaultValue.getType)
    } else {
      Content(content = null, `type` = null)
    }
  }

  implicit def toFeed(feed: com.sun.syndication.feed.synd.SyndFeed) = {
    new Feed(id = feed.getUri, title = feed.getTitle
      , description = feed.getDescriptionEx match { case x: SyndContent => Content(sanitize(x.getValue), x.getType) }
      , items = feed.getEntries.map(_ match { case x: SyndEntry => Item(
        // id = unique(timestamp(x.getPublishedDate), x.getUri),
        uri = x.getUri
        , link = x.getLink
        , author = x.getAuthor
        , title = x.getTitle
        , content = content(x.getContents, x.getDescription)
        , categories = Seq()
        , published = timestamp(x.getPublishedDate)
        , updated = timestamp(x.getUpdatedDate, x.getPublishedDate)
        , alternate = Seq(
          Link(href = x.getLink, `type` = "text/html")
        )
      )})
      , updated = timestamp(feed.getPublishedDate)
    )
  }
}

case class Subscriptions(feeds: Seq[Feed])

@JsonIgnoreProperties(Array("id", "stale"))
case class Feed(id: String
  , title: String = null
  , items: Seq[Item] = Nil
  , updated: Long = 0L
  , description: Content = null
  , status: String = null
  , stale: Boolean = false
  , guid: String = null)

@JsonIgnoreProperties(Array("uri", "link"))
case class Item(uri: String, link: String, author: String, title: String, content: Content
  , categories: Seq[String], published: Long, updated: Long, alternate: Seq[Link])

case class Person(name: String, email: String)

case class Link(href: String, `type`: String)

case class Content(content: String, `type`: String)
