package managers

import kafka.consumer.ConsumerConfig
import java.util.Properties
import kafka.javaapi.consumer.AsyncConsumerConnector
import core.Registry
import Registry.PropertyConstants
import kafka.consumer.async.Consumer
import models.{Status, Server}
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

  Registry.registerObject(PropertyConstants.SERVER_CONNECTIONS, Map[String, ZkClient]())

  override def receive: Actor.Receive = {

    case connectMessage: Message.Connect => {

      val server = connectMessage.server

      val router = Registry.lookupObject(PropertyConstants.ROUTER) match {
        case Some(router: ActorRef) => router
      }

      val serverConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.SERVER_CONNECTIONS) match {
        case serverConnections: Some[Map[String, ZkClient]] => serverConnections.get
      }

      val zkClient = serverConnections.filterKeys(_.toString == connectMessage.server.toString) match {
        case serverExists if serverExists.size > 0 => {
          serverExists.head._2
        }
        case _ => {
          val zkClient = ZkClient.apply(server.address + ":" + server.port.toString, 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
          Registry.registerObject(PropertyConstants.SERVER_CONNECTIONS, Map(server.toString -> zkClient) ++ serverConnections)
          zkClient
        }
      }

      val onSessionEvent: PartialFunction[StateEvent, Unit] = {
        case stateEvent: StateEvent if stateEvent.state.getIntValue == 3 => {
          router ! ConnectNotification(Server(server.address, server.port, server.groupId, Status.CONNECTED.id))
        }
        case stateEvent: StateEvent if stateEvent.state.getIntValue == 0 => {
          router ! ConnectNotification(Server(server.address, server.port, server.groupId, Status.DISCONNECTED.id))
        }
      }

      zkClient.onSessionEvent(onSessionEvent)
      val zookeeperFuture = zkClient.apply()

      zookeeperFuture.onFailure(_ => {
        router ! ConnectNotification(Server(server.address, server.port, server.groupId, Status.DISCONNECTED.id))
        Akka.system.scheduler.scheduleOnce(
          Duration.create(5, TimeUnit.SECONDS), self, Message.Connect(server)
        )
      })

      zookeeperFuture.onSuccess(zookeeper => {
        router ! ConnectNotification(Server(server.address, server.port, server.groupId, Status.CONNECTED.id))
      })
    }

    case Terminated => shutdownConnections()
  }

  private def shutdownConnections() {
    val serverConnections = Registry.lookupObject(PropertyConstants.SERVER_CONNECTIONS)

    serverConnections match {
      case Some(eventConsumerConnectors: List[AsyncConsumerConnector]) => {
        eventConsumerConnectors.map(eventConsumerConnector => eventConsumerConnector.shutdown())
      }
      case _ =>
    }
  }

}