package greader.api

import akka.actor.{Actor, ActorRef, Props}

import spray.http._
import spray.http.MediaTypes._
import spray.http.HttpHeaders._
import spray.http.HttpCharsets._
import spray.httpx.encoding.Gzip
import spray.httpx.SprayJsonSupport._

import spray.routing.{Route, HttpService, RequestContext}
import spray.routing.directives._

class GReaderAPIActor(val scanner: ActorRef) extends Actor with GReaderAPI {
  implicit def actorRefFactory = context

  def receive = runRoute(greaderRoute)
}

