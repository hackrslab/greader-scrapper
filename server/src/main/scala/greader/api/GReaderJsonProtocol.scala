package greader.api

import spray.httpx.marshalling._
import spray.http._
import spray.http.HttpCharsets._
import spray.http.MediaTypes._

import spray.json.{RootJsonFormat, DefaultJsonProtocol}
import spray.json.{JsArray, JsNull, JsObject, JsString, JsNumber, JsValue}
import greader.domain.{Subscriptions, Feed, Item, Content, Link}

import scala.collection.JavaConversions._

import greader.Utils._

object GReaderJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionsFormat extends RootJsonFormat[Subscriptions] {
    def write(s: Subscriptions): JsValue = JsObject(
      "feeds" -> JsArray(
        s.feeds.map(_ match {
          case f => JsObject(
            "id" -> JsString(f.id),
            "title" -> JsString(if (f.title == null) "" else f.title),
            "status" -> JsString(f.status)
          )
        }
        ): _*
      )
    )

    def read(json: JsValue) = json.asJsObject.getFields("feeds") match {
      case e: JsArray => Subscriptions(
        feeds = Seq(e.elements.map(_.asJsObject.getFields("id") match {
          case id: JsString => Feed(id = id.value)
        }): _*)
      )
    }
  }

  implicit object FeedFormat extends RootJsonFormat[Feed] {
    def write(f: Feed): JsValue = JsObject(
      "title" -> JsString(f.title),
      "updated" -> JsNumber(f.updated),
      "description" -> JsObject("content" -> JsString(f.description.content)),
      "fid" -> JsString(encode(unique(f.id)).mkString),
      "items" -> JsArray(f.items.map(_ match {
        case i => JsObject(
          "author" -> JsString(i.author),
          "title" -> JsString(i.title),
          "content" -> JsObject("content" -> JsString(i.content.content)),
          "categories" -> JsArray(i.categories.map(JsString(_)): _*),
          "published" -> JsNumber(i.published),
          "updated" -> JsNumber(i.updated),
          "alternate" -> JsArray(i.alternate.map(_ match {
            case null => JsNull
            case l => JsObject("href" -> JsString(l.href), "type" -> JsString(l.`type`))
          }): _*)
        )
      }): _*)
    )
    def read(json: JsValue) = Feed(id = null)
  }
}
