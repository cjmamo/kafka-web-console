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
import com.twitter.zk.{ZNode, ZkClient}
import scala.util.Random
import okapies.finagle.Kafka
import kafka.api.OffsetRequest

object Topic extends Controller {

  object TopicsWrites extends Writes[Seq[Map[String, Object]]] {
    def writes(l: Seq[Map[String, Object]]) = {
      val topics = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => Json.toJson(e._2.asInstanceOf[Seq[Map[String, String]]])
            case v => Json.toJson(v.toString)
          }
          (e._1, v)
        }.toSeq

        JsObject(js)
      }
      JsArray(topics)
    }
  }

  object TopicWrites extends Writes[Seq[Map[String, Object]]] {
    def writes(l: Seq[Map[String, Object]]) = {
      val topic = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => Json.toJson(e._2.asInstanceOf[Seq[Map[String, String]]])
            case v => Json.toJson(v.toString)
          }
          (e._1, v)
        }.toSeq

        JsObject(js)
      }
      JsArray(topic)
    }
  }

  def index = Action.async {
    val topicsZks = connectedZookeepers { (zk, zkClient) =>
      for {
      // it's possible to have topics without partitions in Zookeeper
        allTopicNodes <- getZChildren(zkClient, "/brokers/topics/*")
        allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
        partitions <- getZChildren(zkClient, "/brokers/topics/*/partitions/*")

        topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))

        topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toSeq

        topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicsAndPartitionsAndZookeeper, zkClient)

      } yield topicsAndPartitionsAndZookeeperAndLogSize
    }

    Future.sequence(topicsZks).map(l => Ok(Json.toJson(l.flatten)(TopicsWrites)))
  }

  def show(topic: String, zookeeper: String) = Action.async {
    val connectedZks = connectedZookeepers((z, c) => (z, c)).filter(_._1.name == zookeeper)

    if (connectedZks.size > 0) {
      val (_, zkClient) = connectedZks.head

      val topicInfo = for {
        leaders <- getPartitionLeaders(topic, zkClient)
        partitionsLogSize <- getPartitionsLogSize(topic, leaders)
        owners <- getPartitionOwners(topic, zkClient)
        consumersAndPartitionOffsets <- getPartitionOffsets(topic, zkClient)
      } yield createTopicInfo(consumersAndPartitionOffsets, partitionsLogSize, owners)

      topicInfo.map(poc => Ok(Json.toJson(poc)(TopicWrites)))
    }
    else {
      Future(Ok(Json.toJson(List[String]())))
    }
  }

  def feed(topic: String, zookeeper: String) = WebSocket.using[String] { implicit request =>

    val topicCountMap = new util.HashMap[EventHandler[String, String], Integer]()
    val zk = models.Zookeeper.findByName(zookeeper).get
    val consumerGroup = "web-console-consumer-" + Random.nextInt(100000)
    val consumer = Consumer.create(createConsumerConfig(zk.toString, consumerGroup))
    val zkClient = Registry.lookupObject(PropertyConstants.ZookeeperConnections).get.asInstanceOf[Map[String, ZkClient]](zookeeper)

    val out = Concurrent.unicast[String] { channel: Concurrent.Channel[String] =>

      val cb = (messageHolder: MessageAndMetadata[String, String]) => {
        channel.push(messageHolder.message)
      }

      getZChildren(zkClient, "/brokers/topics/" + topic + "/partitions/*").map { p =>
        topicCountMap.put(new EventHandler(topic, cb), p.size)
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
    val props = new Properties()
    props.put("zookeeper.connect", zookeeperAddress)
    props.put("group.id", gid)
    props.put("zookeeper.session.timeout.ms", "400")
    props.put("zookeeper.sync.time.ms", "200")
    props.put("auto.commit.interval.ms", "1000")

    return new ConsumerConfig(props)
  }

  private def getPartitionOwners(topicName: String, zkClient: ZkClient): Future[Seq[(String, Int, String)]] = {
    return for {
      owners <- getZChildren(zkClient, "/consumers/*/owners/" + topicName + "/" + "*")
      ownerIds <- Future.sequence(owners.map(z => twitterToScalaFuture(z.getData().map(d => (z.path.split("/")(2), z.path.split("/")(5).toInt, new String(d.bytes))))))
    } yield ownerIds
  }

  private def createTopicInfo(consumersAndPartitionOffsets: Map[String, Seq[Long]], partitionsLogSize: Seq[Long],
                              owners: Seq[(String, Int, String)]): Seq[Map[String, Object]] = {

    consumersAndPartitionOffsets.map { cPO =>
      val offsetSum = cPO._2.map(_.toInt).foldLeft(0)(_ + _)
      val partitionsLogSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toInt

      Map("consumerGroup" -> cPO._1,
          "offset" -> offsetSum.toString,
          "lag" -> (partitionsLogSizeSum - offsetSum).toString,
          "partitions" -> createPartitionInfo(cPO, partitionsLogSize, owners))
    }.toSeq
  }

  private def createTopicsInfo(topics: Seq[Map[String, Object]], zkClient: ZkClient): Future[Seq[Map[String, Object]]] = {
    Future.sequence(topics.map { e =>
      for {
        partitionLeaders <- getPartitionLeaders(e("name").toString, zkClient)
        partitionsLogSize <- getPartitionsLogSize(e("name").toString, partitionLeaders)
        partitions = partitionsLogSize.zipWithIndex.map(pls => Map("id" -> pls._2.toString, "logSize" -> pls._1.toString, "leader" -> partitionLeaders(pls._2)))
        logSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toInt.toString
      } yield Map("name" -> e("name"), "partitions" -> partitions, "zookeeper" -> e("zookeeper"), "logSize" -> logSizeSum)

    })
  }

  private def createPartitionInfo(consumerGroupAndPartitionOffsets: (String, Seq[Long]),
                                  partitionsLogSize: Seq[Long],
                                  owners: Seq[(String, Int, String)]): Seq[Map[String, String]] = {

    consumerGroupAndPartitionOffsets._2.zipWithIndex.map { case (pO, i) =>
      Map("id" -> i.toString, "offset" -> pO.toString, "lag" -> (partitionsLogSize(i) - pO).toString,
        "owner" -> {
          owners.find(o => (o._1 == consumerGroupAndPartitionOffsets._1) && (o._2 == i)) match {
            case Some(s) => s._3
            case None => ""
          }
        })
    }
  }

}
