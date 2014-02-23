package actors

import akka.actor.{Terminated, Actor}
import core.{Message, Registry}
import Registry.PropertyConstants
import play.api.libs.iteratee.{Concurrent, Enumerator}
import models.{Status, Zookeeper}

class DatabaseManager extends Actor {

  override def receive: Actor.Receive = {

    case connectNotification: Message.ConnectNotification => {
      val zk = connectNotification.zookeeper
      Zookeeper.upsert(zk)
    }
    case Terminated => {
      Zookeeper.update(Zookeeper.findAll.map(z => Zookeeper(z.id, z.host, z.port, z.groupId, Status.Disconnected.id)))
    }
  }

}
