package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.{Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.Util._

object Topic extends Controller {

  def index = Action.async {

    val topicsZks = connectedZookeepers { (zk, zkClient) =>
      for {
        partitions <- getZChildren(zkClient, "/brokers/topics/*/partitions/*")
      } yield (zk.name, partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2)))
    }

    topicsZks match {
      case Some(s) if s.size > 0 => Future.sequence(s).map(l => Ok(views.html.topic.index(l)))
      case _ => Future(Ok(views.html.topic.index()))
    }
  }
}
