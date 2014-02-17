package controllers

import play.api.mvc.{SimpleResult, Action, Controller}
import kafka.utils.ZkUtils
import play.api.libs.json.Json
import core.Registry
import Registry.PropertyConstants
import play.api.data.Form
import kafka.javaapi.consumer.AsyncConsumerConnector
import core.Registry
import com.twitter.zk.{ChildrenCallbackPromise, ZkClient}
import com.twitter.conversions.time._
import com.twitter.util.{Throw, Return, JavaTimer}
import scala.concurrent.{Promise, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.I0Itec.zkclient
import models.Server

object Topic extends Controller {

  def index = Action.async {
    implicit request =>

      val servers = models.Server.findByStatusId(models.Status.CONNECTED.id)

      val serverConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.SERVER_CONNECTIONS) match {
        case serverConnections: Some[Map[String, ZkClient]] => serverConnections.get
      }

      val connectedServers = servers.filter(server => serverConnections.contains(server.toString))

      val topics = connectedServers.map(server => {
        val zkClient = serverConnections.get(server.toString).get
        val zNode = zkClient.apply("/brokers/topics")
        twitterToScalaFuture(zNode.getChildren.apply().map(node => node.children.map(child => {
          val topicAndPartitions = zkClient.apply("/brokers/topics/" + child.name + "/partitions").getChildren.apply().map(partitions => {
            (child.name, partitions.children.size)
          })
          twitterToScalaFuture(topicAndPartitions)
        })))
      }).toList

      if (topics.size > 0) {
        Future.reduce(topics)((topic, allTopics) => {
          topic ++ allTopics
        }).flatMap(topic => {
          Future.sequence(topic).map(t => Ok(views.html.topic.index(t)))
        })
      }
      else {
        Future(Ok(views.html.topic.index()))
      }




    //    topics

    //    val topics = serverConnections match {
    //      case Some(l: List[AsyncConsumerConnector]) => {
    //        l.map( serverConnection => {
    //          serverConnection.getZookeeperClient.
    //        })
    //      }
    //      case _ => Nil
    //    }
    //
    //    val topics = serverClient match {
    //      case Some(zkClient: ZkClient) => ZkUtils.getAllTopics(zkClient)
    //      case _ => Nil
    //    }

    //      render {
    //        case Accepts.Html() => {
    //          Ok(views.html.topic.index())
    //        }
    //        case Accepts.Json() => {
    //          Ok
    //          //        Ok(Json.toJson(topics))
    //        }
    //      }

  }

  private def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

}
