package actors

import akka.actor.Actor
import common.{Message, Registry}
import Registry.PropertyConstants
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.Json

class ClientManager extends Actor {
  override def receive: Actor.Receive = {
    case connectNotification: Message.ConnectNotification => {
      Registry.lookupObject(PropertyConstants.BroadcastChannel) match {
        case Some(broadcastChannel: (Enumerator[String], Concurrent.Channel[String])) => {
          broadcastChannel._2.push(Json.toJson(connectNotification.zookeeper).toString())
        }
        case _ =>
      }
    }
  }
}
