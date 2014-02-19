package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.{Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.Util._

object Topic extends Controller {

  val BrokerTopicsPath = "/brokers/topics"
  val PartitionsPath = "/partitions"

  def index = Action.async {

    val topics = connectedZookeepers { (zk, zkClient) =>
      for {
        topics <- twitterToScalaFuture(zkClient(BrokerTopicsPath).getChildren().map(topicsNode => topicsNode.children))
        topicAndPartitions <- Future.sequence(topics.map { t =>
          twitterToScalaFuture(zkClient(BrokerTopicsPath + "/" + t.name + PartitionsPath).getChildren().map((t.name, _)))
        })
      } yield topicAndPartitions.map(ps => (zk.name, ps._1, ps._2.children.size))
    }

    topics match {
      case Some(ts) if ts.size > 0 => Future.sequence(ts).map(l => Ok(views.html.topic.index(l.flatten)))
      case _ => Future(Ok(views.html.topic.index()))
    }
  }
}
