package controllers

import play.api.mvc.{Action, Controller}
import com.twitter.zk.ZkClient
import core.Registry
import core.Registry.PropertyConstants
import scala.concurrent.{Promise, Future}
import com.twitter.util.{Throw, Return}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.parsing.json.JSON
import util.Util

object Broker extends Controller {

  def index = Action.async {
    request =>

      val zookeepers = models.Zookeeper.findByStatusId(models.Status.Connected.id)

      val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
        case c: Some[Map[String, ZkClient]] => c.get
      }

      val connectedZks = zookeepers.filter(zk => zkConnections.contains(zk.toString))

      val brokers = connectedZks.map {
        zk =>
          val zkClient = zkConnections.get(zk.toString).get
          val zNode = zkClient.apply("/brokers/ids")
          Util.twitterToScalaFuture(zNode.getChildren.apply().map {
            brokerIdsChild => brokerIdsChild.children.map(brokerId =>
              Util.twitterToScalaFuture(zkClient.apply(brokerId.path).getData.apply().map {
                broker =>
                  scala.util.parsing.json.JSON.parseFull(new String(broker.bytes)).get.asInstanceOf[Map[String, Any]]
              })
            )
          })
      }.toList

      if (brokers.size > 0) {
        Future.reduce(brokers)((topic, allTopics) => {
          topic ++ allTopics
        }).flatMap(topic => {
          Future.sequence(topic).map(t => {
            Ok(views.html.broker.index(t))
          })
        })
      }
      else {
        Future(Ok(views.html.topic.index()))
      }
  }

}
