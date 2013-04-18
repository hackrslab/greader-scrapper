package greader

import akka.actor.{ActorSystem, Props}
import akka.routing.FromConfig
import spray.can.server.SprayCanHttpServerApp

import greader.api.GReaderAPIActor

object Boot extends App with SprayCanHttpServerApp {
  implicit override lazy val system = ActorSystem("Main")

  val scanner = system.actorOf(Props[Scanner].withRouter(new FromConfig()), "scanner")
  val service = system.actorOf(Props(new GReaderAPIActor(scanner)).withRouter(new FromConfig()), "GReader")

  newHttpServer(service) ! Bind(interface = "0.0.0.0", port = 8891)
}
