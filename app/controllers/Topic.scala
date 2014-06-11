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
import common.{Message, Registry}
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
import kafka.utils.{ZkUtils, ZKStringSerializer}
import play.api.data.{Forms, Form}
import play.api.libs.concurrent.Akka
import play.api.data.Forms._
import play.api.libs.json.JsArray
import scala.Some
import kafka.message.MessageAndMetadata
import play.api.libs.json.JsObject

object Topic extends Controller {

  val topicForm = Forms.tuple(
    "name" -> Forms.text,
    "group" -> Forms.text,
    "zookeeper" -> Forms.text,
    "partitions" -> Forms.number,
    "replications" -> Forms.number
  )

  object TopicsWrites extends Writes[List[Map[String, Object]]] {
    def writes(l: List[Map[String, Object]]) = {
      val topics = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => Json.toJson(e._2.asInstanceOf[Seq[Map[String, String]]])
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
      val topic = l.map { t =>

        val js = t.map { e =>
          val v = e._2 match {
            case v: Seq[_] => Json.toJson(e._2.asInstanceOf[Seq[Map[String, String]]])
            case v => Json.toJson(v.toString)
          }
          (e._1, v)
        }.toList

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

        topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toList

        topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicsAndPartitionsAndZookeeper, zkClient)

      } yield topicsAndPartitionsAndZookeeperAndLogSize
    }

    Future.sequence(topicsZks).map(l => Ok(Json.toJson(l.flatten)(TopicsWrites)))
  }

  def show(name: String, zookeeper: String) = Action.async {
    val connectedZks = connectedZookeepers((z, c) => (z, c)).filter(_._1.name == zookeeper)

    if (connectedZks.size > 0) {
      val (_, zkClient) = connectedZks.head

      val topicInfo = for {
        leaders <- getPartitionLeaders(name, zkClient)
        partitionsLogSize <- getPartitionsLogSize(name, leaders)
        owners <- getPartitionOwners(name, zkClient)
        consumersAndPartitionsAndOffsets <- getPartitionOffsets(name, zkClient)
      } yield createTopicInfo(consumersAndPartitionsAndOffsets, partitionsLogSize, owners)

      topicInfo.map(poc => Ok(Json.toJson(poc)(TopicWrites)))
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

  def create() = Action { implicit request =>
    val result = Form(topicForm).bindFromRequest.fold(
      formFailure => BadRequest,
      formSuccess => {

        val name: String = formSuccess._1
        val group: String = formSuccess._2
        val zookeeper: String = formSuccess._3
        val partitions: Int = formSuccess._4
        val replications: Int = formSuccess._5

        import org.I0Itec.zkclient.ZkClient

        val zk = models.Zookeeper.findById(zookeeper).get

        val zkClient = new ZkClient(zk.host+":"+zk.port, 30000, 30000, ZKStringSerializer)
        try {
          kafka.admin.AdminUtils.createTopic(zkClient, name, partitions, replications)

        } finally {
          zkClient.close()
        }

        Ok
      }

    )

    result
  }

  def delete(name: String, zookeeper: String) = Action {

    import org.I0Itec.zkclient.ZkClient

    val zk = models.Zookeeper.findById(zookeeper).get

    val zkClient = new ZkClient(zk.host+":"+zk.port, 30000, 30000, ZKStringSerializer)
    try {
      //How it is deleted in the DeleteTopicCommand
      zkClient.deleteRecursive(ZkUtils.getTopicPath(name))
      //kafka.admin.AdminUtils.deleteTopic(zkClient, name)

    } finally {
      zkClient.close()
    }
    Ok
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

  private def getPartitionLeaders(topicName: String, zkClient: ZkClient): Future[Seq[String]] = {
    return for {
      partitionStates <- getZChildren(zkClient, "/brokers/topics/" + topicName + "/partitions/*/state")
      partitionsData <- Future.sequence(partitionStates.map(p => twitterToScalaFuture(p.getData())))
      brokerIds = partitionsData.map(d => scala.util.parsing.json.JSON.parseFull(new String(d.bytes)).get.asInstanceOf[Map[String, Any]].get("leader").get)
      brokers <- Future.sequence(brokerIds.map(bid => getZChildren(zkClient, "/brokers/ids/" + bid.toString.toDouble.toInt)))
      brokersData <- Future.sequence(brokers.flatten.map(d => twitterToScalaFuture(d.getData())))
      brokersInfo = brokersData.map(d => scala.util.parsing.json.JSON.parseFull(new String(d.bytes)).get.asInstanceOf[Map[String, Any]])
    } yield brokersInfo.map(bi => bi.get("host").get + ":" + bi.get("port").get.toString.toDouble.toInt)
  }

  private def getPartitionsLogSize(topicName: String, partitionLeaders: Seq[String]): Future[Seq[Long]] = {
    return for {
      clients <- Future.sequence(partitionLeaders.map(addr => Future(Kafka.newRichClient(addr))))
      partitionsLogSize <- Future.sequence(clients.zipWithIndex.map { e =>
        val offset = twitterToScalaFuture(e._1.offset(topicName, e._2, OffsetRequest.LatestTime)).map(_.offsets.head)
        e._1.close()
        offset
      })
    } yield partitionsLogSize
  }

  private def getPartitionOwners(topicName: String, zkClient: ZkClient): Future[Seq[(String, String, String)]] = {
    return for {
      owners <- getZChildren(zkClient, "/consumers/*/owners/" + topicName + "/" + "*")
      ownerIds <- Future.sequence(owners.map(z => twitterToScalaFuture(z.getData().map(d => (z.path.split("/")(2), z.path.split("/")(5), new String(d.bytes))))))
    } yield ownerIds
  }

  private def getPartitionOffsets(topicName: String, zkClient: ZkClient): Future[Seq[(String, String, String)]] = {
    return for {
      offsetsPartitionsNodes <- getZChildren(zkClient, "/consumers/*/offsets/" + topicName + "/*")
      consumersAndPartitionsAndOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(2), p.name, new String(d.bytes))))))
    } yield consumersAndPartitionsAndOffsets
  }

  private def createTopicInfo(consumersAndPartitionsAndOffsets: Seq[(String, String, String)], partitionsLogSize: Seq[Long],
                              owners: Seq[(String, String, String)]): List[Map[String, Object]] = {
    consumersAndPartitionsAndOffsets.groupBy(_._1).map { t =>
      val offsetSum = t._2.map(_._3.toInt).foldLeft(0)(_ + _)
      val partitionsLogSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toInt

      Map("consumerGroup" -> t._1, "offset" -> offsetSum.toString, "lag" -> (partitionsLogSizeSum - offsetSum).toString,
        "partitions" -> t._2.map { p =>
          Map("id" -> p._2, "offset" -> p._3, "lag" -> (partitionsLogSize(p._2.toInt) - p._3.toInt).toString,
            "owner" -> {
              owners.find(o => (o._1 == t._1) && (o._2 == p._2)) match {
                case Some(s) => s._3
                case None => ""
              }
            })
        })
    }.toList
  }

  private def createTopicsInfo(topics: List[Map[String, Object]], zkClient: ZkClient): Future[List[Map[String, Object]]] = {
    Future.sequence(topics.map { e =>
      for {
        partitionLeaders <- getPartitionLeaders(e("name").toString, zkClient)
        partitionsLogSize <- getPartitionsLogSize(e("name").toString, partitionLeaders)
        partitions = partitionsLogSize.zipWithIndex.map(pls => Map("id" -> pls._2.toString, "logSize" -> pls._1.toString))
        logSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toInt.toString
      } yield Map("name" -> e("name"), "partitions" -> partitions, "zookeeper" -> e("zookeeper"), "logSize" -> logSizeSum)

    })
  }

}
