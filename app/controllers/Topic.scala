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
import models.Zookeeper
import util.Util

object Topic extends Controller {

  val BrokerTopicsPath = "/brokers/topics"
  val PartitionsPath = "/partitions"

  def index = Action.async {
      val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

      val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
        case c: Some[Map[String, ZkClient]] => c.get
      }

      val topics = connectedZks.map {
        zk =>
          val zkClient = zkConnections.get(zk.id).get
          val topicsNode = zkClient(BrokerTopicsPath)
          Util.twitterToScalaFuture(topicsNode.getChildren().map {
            topics => topics.children.map {
              topic =>
                val topicAndPartitions = zkClient(BrokerTopicsPath + "/" + topic.name + PartitionsPath).getChildren().map {
                  partitions =>
                    (zk.name, topic.name, partitions.children.size)
                }
                Util.twitterToScalaFuture(topicAndPartitions)
            }
          })
      }.toList

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
  }
}
