package managers

import kafka.consumer.ConsumerConfig
import java.util.Properties
import kafka.javaapi.consumer.AsyncConsumerConnector
import core.Registry
import Registry.PropertyConstants
import kafka.consumer.async.Consumer
import models.{Status, Zookeeper}
import akka.actor.{ActorRef, Terminated, Actor}
import play.api.libs.iteratee.Enumerator
import router.Message
import Message.ConnectNotification
import com.twitter.zk._
import com.twitter.util.{Future, JavaTimer}
import com.twitter.conversions.time._
import kafka.utils.ZkUtils
import scala.annotation.tailrec
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import router.Message.ConnectNotification
import akka.actor.Terminated
import scala.Some
import router.Message.ConnectNotification
import akka.actor.Terminated
import scala.Some

class ConnectionManager() extends Actor {

  Registry.registerObject(PropertyConstants.ZookeeperConnections, Map[String, ZkClient]())

  override def receive: Actor.Receive = {

    case connectMessage: Message.Connect => {

      val zk = connectMessage.zookeeper

      val router = Registry.lookupObject(PropertyConstants.Router) match {
        case Some(router: ActorRef) => router
      }

      val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
        case zkConnections: Some[Map[String, ZkClient]] => zkConnections.get
      }

      val zkClient = zkConnections.filterKeys(_.toString == connectMessage.zookeeper.toString) match {
        case zk if zk.size > 0 => {
          zk.head._2
        }
        case _ => {
          val zkClient = ZkClient.apply(zk.host + ":" + zk.port.toString, 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
          Registry.registerObject(PropertyConstants.ZookeeperConnections, Map(zk.toString -> zkClient) ++ zkConnections)
          zkClient
        }
      }

      val onSessionEvent: PartialFunction[StateEvent, Unit] = {
        case stateEvent: StateEvent if stateEvent.state.getIntValue == 3 => {
          router ! ConnectNotification(Zookeeper(zk.host, zk.port, zk.groupId, Status.Connected.id))
        }
        case stateEvent: StateEvent if stateEvent.state.getIntValue == 0 => {
          router ! ConnectNotification(Zookeeper(zk.host, zk.port, zk.groupId, Status.Disconnected.id))
        }
      }

      zkClient.onSessionEvent(onSessionEvent)
      val zookeeperFuture = zkClient.apply()

      zookeeperFuture.onFailure(_ => {
        router ! ConnectNotification(Zookeeper(zk.host, zk.port, zk.groupId, Status.Disconnected.id))
        Akka.system.scheduler.scheduleOnce(
          Duration.create(5, TimeUnit.SECONDS), self, Message.Connect(zk)
        )
      })

      zookeeperFuture.onSuccess(zookeeper => {
        router ! ConnectNotification(Zookeeper(zk.host, zk.port, zk.groupId, Status.Connected.id))
      })
    }

    case Terminated => shutdownConnections()
  }

  private def shutdownConnections() {
    val zkConnections = Registry.lookupObject(PropertyConstants.ZookeeperConnections)

    zkConnections match {
      case Some(eventConsumerConnectors: List[AsyncConsumerConnector]) => {
        eventConsumerConnectors.map(eventConsumerConnector => eventConsumerConnector.shutdown())
      }
      case _ =>
    }
  }

}