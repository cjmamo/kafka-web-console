package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.Util._
import scala.Some

object Broker extends Controller {

  def index = Action.async {

    val brokers = connectedZookeepers { (zk, zkClient) =>

      for {
        brokerIds <- getZChildren(zkClient, "/brokers/ids/*")
        brokers <- Future.sequence(brokerIds.map(brokerId => twitterToScalaFuture(brokerId.getData())))
      } yield brokers.map(b => (zk, scala.util.parsing.json.JSON.parseFull(new String(b.bytes)).get.asInstanceOf[Map[String, Any]]))

    }

    brokers match {
      case Some(bs) if bs.size > 0 => Future.sequence(bs).map(l => Ok(views.html.broker.index(l.flatten)))
      case _ => Future(Ok(views.html.broker.index()))
    }
  }

}
