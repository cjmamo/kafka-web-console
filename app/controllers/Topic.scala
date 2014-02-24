package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import scala.concurrent.{Channel, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import common.Util._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import common.Registry
import common.Registry.PropertyConstants
import java.util
import kafka.javaapi.consumer.EventHandler
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder
import kafka.consumer.async.Consumer
import kafka.consumer.ConsumerConfig
import java.util.Properties
import com.twitter.zk.ZkClient

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

  def show(name: String, zookeeper: String) = Action.async {
    val connectedZks = connectedZookeepers((z, c) => (z, c)).filter(_._1.name == zookeeper)

    if (connectedZks.size > 0) {
      val (_, zkClient) = connectedZks.head
      val partitionsOffsetsAndConsumersFuture = for {
      // it's possible that a offset dir hasn't been created yet for some consumers
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

  def feed(name: String, zookeeper: String) = WebSocket.using[String] { implicit request =>

    val topicCountMap = new util.HashMap[EventHandler[String, String], Integer]()
    val consumer = Consumer.create(createConsumerConfig(models.Zookeeper.findById(zookeeper).get.toString, "1234"))
    val zkClient = Registry.lookupObject(PropertyConstants.ZookeeperConnections).get.asInstanceOf[Map[String, ZkClient]](models.Zookeeper.findById(zookeeper).get.name)

    val out = Concurrent.unicast[String] { channel: Concurrent.Channel[String] =>

      val cb = (messageHolder: MessageAndMetadata[String, String]) => {
        play.Logger.error(messageHolder.message)
        channel.push(messageHolder.message)
      }

      getZChildren(zkClient, "/brokers/topics/" + name + "/partitions/*").map { p =>
        for (n <- 0 to p.size - 1) {
          topicCountMap.put(new EventHandler(name, cb), n)
        }

        consumer.createMessageStreams(topicCountMap, new StringDecoder(), new StringDecoder())
      }

    }

    val in = Iteratee.foreach[String](println).map { _ =>
      consumer.commitOffsets()
      consumer.shutdown()
    }

    (in, out)
  }

  private def createConsumerConfig(a_zookeeper: String, a_groupId: String): ConsumerConfig = {
    val props = new Properties();
    props.put("zookeeper.connect", a_zookeeper);
    props.put("group.id", a_groupId);
    props.put("zookeeper.session.timeout.ms", "400");
    props.put("zookeeper.sync.time.ms", "200");
    props.put("auto.commit.interval.ms", "1000");

    return new ConsumerConfig(props);
  }

}
