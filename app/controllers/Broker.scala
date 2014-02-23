package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import core.Util._
import play.api.libs.json.{Writes, Json}
import scala.Some

object Broker extends Controller {

  implicit object BrokerWrites extends Writes[List[(String, Map[String, Any])]] {
    def writes(l: List[(String, Map[String, Any])]) = {
      val brokers = l.map { i =>

        val fields = i._2.map { kv =>
          kv._2 match {
            case v: Double => (kv._1, v.toInt.toString)
            case _ => (kv._1, kv._2.toString())
          }
        }

        fields + ("zookeeper" -> i._1)
      }
      Json.toJson(brokers)
    }
  }

  def index = Action.async {

    val brokers = connectedZookeepers { (zk, zkClient) =>

      for {
        brokerIds <- getZChildren(zkClient, "/brokers/ids/*")

        brokers <- Future.sequence(brokerIds.map(brokerId => twitterToScalaFuture(brokerId.getData())))
      } yield brokers.map(b => (zk.name, scala.util.parsing.json.JSON.parseFull(new String(b.bytes)).get.asInstanceOf[Map[String, Any]]))

    }

    Future.sequence(brokers).map(l => Ok(Json.toJson(l.flatten)))
  }

}
