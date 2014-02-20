package managers

import kafka.javaapi.consumer.AsyncConsumerConnector
import core.Registry
import Registry.PropertyConstants
import models.{Status, Zookeeper}
import akka.actor.{ActorRef, Actor}
import router.Message
import com.twitter.zk._
import com.twitter.util.{JavaTimer}
import com.twitter.conversions.time._
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import router.Message.ConnectNotification
import akka.actor.Terminated
import scala.Some
import org.apache.zookeeper.Watcher.Event.KeeperState

class ConnectionManager() extends Actor {

  Registry.registerObject(PropertyConstants.ZookeeperConnections, Map[String, ZkClient]())

  override def receive: Actor.Receive = {

    case connectMessage: Message.Connect => {

      val zk = connectMessage.zookeeper

      val router = Registry.lookupObject(PropertyConstants.Router) match {
        case Some(router: ActorRef) => router
      }

      val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
        case Some(zkConnections: Map[_, _]) => zkConnections.asInstanceOf[Map[String, ZkClient]]
      }

      val zkClient = zkConnections.filterKeys(_ == connectMessage.zookeeper.id) match {
        case zk if zk.size > 0 => {
          zk.head._2
        }
        case _ => {
          val zkClient = ZkClient(zk.host + ":" + zk.port.toString, 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
          Registry.registerObject(PropertyConstants.ZookeeperConnections, Map(zk.id -> zkClient) ++ zkConnections)
          zkClient
        }
      }

      val onSessionEvent: PartialFunction[StateEvent, Unit] = {
        case s: StateEvent if s.state == KeeperState.SyncConnected => {
          router ! ConnectNotification(Zookeeper(zk.name, zk.host, zk.port, zk.groupId, Status.Connected.id))
        }
        case s: StateEvent if s.state == KeeperState.Disconnected => {
          router ! ConnectNotification(Zookeeper(zk.name, zk.host, zk.port, zk.groupId, Status.Disconnected.id))
        }
        case s: StateEvent if s.state == KeeperState.Expired => {
          router ! ConnectNotification(Zookeeper(zk.name, zk.host, zk.port, zk.groupId, Status.Disconnected.id))
        }
      }

      zkClient.onSessionEvent(onSessionEvent)
      val zookeeperFuture = zkClient()

      zookeeperFuture.onFailure(_ => {
        router ! ConnectNotification(Zookeeper(zk.name, zk.host, zk.port, zk.groupId, Status.Disconnected.id))
        Akka.system.scheduler.scheduleOnce(
          Duration.create(5, TimeUnit.SECONDS), self, Message.Connect(zk)
        )
      })
    }

    case Terminated => shutdownConnections()
  }

  private def shutdownConnections() {
    val zkConnections = Registry.lookupObject(PropertyConstants.ZookeeperConnections)

    zkConnections match {
      case Some(eventConsumerConnectors: List[_]) => {
        eventConsumerConnectors.asInstanceOf[List[AsyncConsumerConnector]].map(eventConsumerConnector => eventConsumerConnector.shutdown())
      }
      case _ =>
    }
  }

}