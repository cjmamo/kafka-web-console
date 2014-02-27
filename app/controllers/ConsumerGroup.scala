package controllers

import play.api.mvc.{Action, Controller}
import common.Util._
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ConsumerGroup extends Controller {

  def show(consumerGroup: String, topic: String, zookeeper: String) = Action.async {
    val connectedZks = connectedZookeepers((z, c) => (z, c)).filter(_._1.name == zookeeper)

    if (connectedZks.size > 0) {
      val (_, zkClient) = connectedZks.head
      val consumerIdsFuture = for {
        consumers <- getZChildren(zkClient, "/consumers/" + consumerGroup + "/ids/*")
        consumersData <- Future.sequence(consumers.map(c => twitterToScalaFuture(c.getData())))
        consumersMaps = consumersData.map(d => (d.name, scala.util.parsing.json.JSON.parseFull(new String(d.bytes)).get.asInstanceOf[Map[String, Any]]))
        topicConsumerMaps = consumersMaps.filter { c =>
          c._2("subscription").asInstanceOf[Map[String, String]].get(topic) match {
            case Some(_) => true
            case _ => false
          }
        }
      } yield topicConsumerMaps.map(_._1)

      consumerIdsFuture.map(consumerIds => Ok(Json.toJson(consumerIds)))
    }
    else {
      Future(Ok(Json.toJson(List[String]())))
    }
  }

}
