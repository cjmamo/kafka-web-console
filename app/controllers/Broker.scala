package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.Util._

object Broker extends Controller {

  def index = Action.async {

    val brokers = connectedZookeepers { (zk, zkClient) =>

      for {
        brokerIds <- twitterToScalaFuture(zkClient("/brokers/ids").getChildren().map(_.children))
        brokers <- Future.sequence(brokerIds.map(brokerId => twitterToScalaFuture(zkClient(brokerId.path).getData())))
      } yield brokers.map(b => (zk, scala.util.parsing.json.JSON.parseFull(new String(b.bytes)).get.asInstanceOf[Map[String, Any]]))

    }.toList

    if (brokers.size > 0) {
      Future.sequence(brokers).map(l => Ok(views.html.broker.index(l.flatten)))
    }
    else {
      Future(Ok(views.html.topic.index()))
    }
  }

}
