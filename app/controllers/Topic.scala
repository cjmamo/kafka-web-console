package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import core.Util._
import play.api.libs.json._
import scala.util.parsing.json.JSONArray
import play.api.libs.json.JsObject

object Topic extends Controller {

  object TopicsWrites extends Writes[List[Map[String, Object]]] {
    def writes(l: List[Map[String, Object]]) = {
      val topics = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => Json.toJson(e._2.asInstanceOf[Seq[String]])
            case v => Json.toJson(v.toString)
          }
          (e._1, v)
        }.toList

        JsObject(js)
      }
      JsArray(topics)
    }
  }

  object TopicWrites extends Writes[List[Map[String, Object]]] {
    def writes(l: List[Map[String, Object]]) = {
      val topics = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => {
              Json.toJson(e._2.asInstanceOf[Seq[Map[String, String]]])
            }
            case v => Json.toJson(v.toString)
          }
          (e._1, v)
        }.toList

        JsObject(js)
      }
      JsArray(topics)
    }
  }

  def index = Action.async {
    val topicsZks = connectedZookeepers { (zk, zkClient) =>
      for {
        partitions <- getZChildren(zkClient, "/brokers/topics/*/partitions/*")
        topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
      } yield topics.map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toList
    }

    Future.sequence(topicsZks).map(l => Ok(Json.toJson(l.flatten)(TopicsWrites)))
  }

  def show(name: String) = Action.async { request =>
    request.body.asJson match {
      case Some(s) => getTopic(name, s)
      case _ => Future(BadRequest(""))
    }
  }

  private def getTopic(name: String, zookeeper: JsValue) = {
    val connectedZks = connectedZookeepers((z, c) => (z, c)).filter(_._1.name == (zookeeper \ "zookeeper").as[String])

    if (connectedZks.size > 0) {
      val (_, zkClient) = connectedZks.head
      val partitionsOffsetsAndConsumersFuture = for {
        // it's possible that a offset dir hasn't been created yet for this consumer
        ownedTopicNodes <- getZChildren(zkClient, "/consumers/*/owners/" + name)

        allConsumers = ownedTopicNodes.map(n => n.path.split("/").filter(_ != "")(1))
        offsetsPartitionsNodes <- getZChildren(zkClient, "/consumers/*/offsets/" + name + "/*")
        consumersAndPartitionsAndOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(2), p.name, new String(d.bytes))))))
        partitionsOffsetsAndConsumers = consumersAndPartitionsAndOffsets.groupBy(_._1).map { s =>
          Map("consumerGroup" -> s._1, "offsets" -> s._2.map { t =>
            Map("partition" -> t._2, "offset" -> t._3)
          })
        }.toList
        diff = allConsumers.filterNot { ac =>
          partitionsOffsetsAndConsumers.map(c => ac == c("consumerGroup")).contains(true)
        }

      } yield diff.map(cg => Map("consumerGroup" -> cg, "offsets" -> Nil)).toList ++ partitionsOffsetsAndConsumers
      partitionsOffsetsAndConsumersFuture.map(poc => Ok(Json.toJson(poc)(TopicWrites)))
    }
    else {
      Future(Ok(Json.toJson(List[String]())))
    }
  }

}
