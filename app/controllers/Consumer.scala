package controllers

import play.api.mvc.{Action, Controller}
import play.api.data.Form
import com.twitter.zk.ZkClient
import core.Registry
import core.Registry.PropertyConstants
import scala.concurrent.Future
import util.Util
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Consumer extends Controller {

  def show(zookeeper: String, topic: String) = Action.async {

    val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

    val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
      case c: Some[Map[String, ZkClient]] => c.get
    }

    val allZkConsumersList = connectedZks.map {
      zk =>
        val zkClient = zkConnections.get(zk.id).get
        val consumersFuture = zkClient("/consumers").getChildren().map(node => node.children)
        val consumersAndTopicsTwitterFuture = consumersFuture.map(consumers => consumers.map(c => zkClient("/consumers/" + c.name + "/owners").getChildren().map(t => Map(c.name -> t.children))))
        val consumersAndTopicsFuture = Util.twitterToScalaFuture(consumersAndTopicsTwitterFuture)
        val consumersAndTopicsFlattenedFuture = consumersAndTopicsFuture.flatMap(consumersAndTopics => Future.sequence(consumersAndTopics.map(i => Util.twitterToScalaFuture(i))))

        consumersAndTopicsFlattenedFuture.map {
          consumersAndTopicsFlattened => consumersAndTopicsFlattened.filter {
            consumerAndTopics =>
              consumerAndTopics.head._2.filter(t => t.name == topic).size > 0
          }.map(filtered => filtered.head._1)
        }
    }.toList

    Future.sequence(allZkConsumersList).map {
      allZksList =>
        Ok(views.html.consumer.show(allZksList.flatten))
    }

  }

}
