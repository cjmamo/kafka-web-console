package managers

import akka.actor.Actor
import core.Registry
import Registry.PropertyConstants
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.Json
import router.Message

class ClientManager extends Actor {
  override def receive: Actor.Receive = {
    case connectNotification: Message.ConnectNotification => {
      Registry.lookupObject(PropertyConstants.BROADCAST_CHANNEL) match {
        case Some(broadcastChannel: (Enumerator[String], Concurrent.Channel[String])) => broadcastChannel._2.push(Json.toJson(connectNotification.server).toString())
        case _ =>
      }
    }
  }
}
