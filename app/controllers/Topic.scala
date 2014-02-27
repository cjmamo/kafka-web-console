/*
 * Copyright 2014 Claude Mamo
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import common.Util._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.iteratee.{Concurrent, Iteratee}
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
import scala.util.Random

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
      // possible for topics without partitions in Zookeeper
        allTopicNodes <- getZChildren(zkClient, "/brokers/topics/*")
        allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
        partitions <- getZChildren(zkClient, "/brokers/topics/*/partitions/*")
        topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
      } yield (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toList
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
    val zk = models.Zookeeper.findById(zookeeper).get
    val consumerGroup = "web-console-consumer-" + Random.nextInt(100000)
    val consumer = Consumer.create(createConsumerConfig(zk.toString, consumerGroup))
    val zkClient = Registry.lookupObject(PropertyConstants.ZookeeperConnections).get.asInstanceOf[Map[String, ZkClient]](models.Zookeeper.findById(zookeeper).get.name)

    val out = Concurrent.unicast[String] { channel: Concurrent.Channel[String] =>

      val cb = (messageHolder: MessageAndMetadata[String, String]) => {
        channel.push(messageHolder.message)
      }

      getZChildren(zkClient, "/brokers/topics/" + name + "/partitions/*").map { p =>
        topicCountMap.put(new EventHandler(name, cb), p.size)
        consumer.createMessageStreams(topicCountMap, new StringDecoder(), new StringDecoder())
      }

    }

    val in = Iteratee.foreach[String](println).map { _ =>
      consumer.commitOffsets()
      consumer.shutdown()
      deleteZNode(zkClient, "/consumers/" + consumerGroup)
    }

    (in, out)
  }

  private def createConsumerConfig(zookeeperAddress: String, gid: String): ConsumerConfig = {
    val props = new Properties();
    props.put("zookeeper.connect", zookeeperAddress);
    props.put("group.id", gid);
    props.put("zookeeper.session.timeout.ms", "400");
    props.put("zookeeper.sync.time.ms", "200");
    props.put("auto.commit.interval.ms", "1000");

    return new ConsumerConfig(props);
  }

}
