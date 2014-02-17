package managers

import akka.actor.Actor
import router.Message
import Message.ConnectNotification
import core.Registry
import Registry.PropertyConstants
import play.api.libs.iteratee.{Concurrent, Enumerator}
import models.{Status, Server}

class DatabaseManager extends Actor {

  override def receive: Actor.Receive = {

    case connectNotification: Message.ConnectNotification => {
      val server = connectNotification.server
      Server.upsert(server)
    }
  }

}
