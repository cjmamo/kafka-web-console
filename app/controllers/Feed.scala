package controllers

import play.api.mvc.{WebSocket, Controller}
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import core.Registry
import Registry.PropertyConstants
import akka.actor.ActorRef
import core.Registry

object Feed extends Controller {

  def index() = WebSocket.using[String] { implicit request =>

    val in = Iteratee.ignore[String]

    val out = Registry.lookupObject(PropertyConstants.BroadcastChannel) match {
      case Some(broadcastChannel: (Enumerator[String], Concurrent.Channel[String])) => broadcastChannel._1
      case _ => Enumerator.empty[String]
    }

    (in, out)
  }

}
