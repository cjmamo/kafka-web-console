/*
 * Copyright 2014 Claude Mamo
 * Some changes Copyright 2014 Isaac Banner | ibanner56
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

package common

import kafka.consumer.async.AsyncLowLevelConsumer
import play.api.Logger

import scala.concurrent.{Future, Promise}
import com.twitter.util.{Throw, Return}
import com.twitter.zk.{ZNode, ZkClient}
import common.Registry.PropertyConstants
import models.Zookeeper
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.apache.zookeeper.KeeperException.NoNodeException

object Util {

  def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

  def getPartitionLeaders(topicName: String, zkClient: ZkClient): Future[Seq[String]] = {
    Logger.debug("Getting partition leaders for topic " + topicName)
    return for {
      partitionStates <- getZChildren(zkClient, "/brokers/topics/" + topicName + "/partitions/*/state")
      partitionsData <- Future.sequence(partitionStates.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(5), new String(d.bytes))))))
      brokerIds = partitionsData.map(d => (d._1, scala.util.parsing.json.JSON.parseFull(d._2).get.asInstanceOf[Map[String, Any]].get("leader").get))
      brokers <- Future.sequence(brokerIds.map(bid => getZChildren(zkClient, "/brokers/ids/" + bid._2.toString.toDouble.toInt).map((bid._1, _))))
      partitionsWithLeaders = brokers.filter(_._2.headOption match {
        case Some(s) => true
        case _ => false
      })
      partitionsWithoutLeaders = brokers.filterNot(b => b._2.headOption match {
        case Some(s) => true
        case _ => Logger.warn("Partition " + b._1 + " in topic " + topicName + " has no leaders"); false
      })
      brokersData <- Future.sequence(partitionsWithLeaders.map(d => twitterToScalaFuture(d._2.head.getData().map((d._1, _)))))
      brokersInfo = brokersData.map(d => (d._1, scala.util.parsing.json.JSON.parseFull(new String(d._2.bytes)).get.asInstanceOf[Map[String, Any]]))
      brokersAddr = brokersInfo.map(bi => (bi._1, bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))
      pidsAndBrokers = brokersAddr ++ partitionsWithoutLeaders.map(pid => (pid._1, ""))
    } yield pidsAndBrokers.sortBy(pb => pb._1.toInt).map(pb => pb._2)
  }

  def getPartitionsLogSize(topicName: String, partitionLeaders: Seq[String]): Future[Seq[Long]] = {
    Logger.debug("Getting partition log sizes for topic " + topicName + " from partition leaders " + partitionLeaders.mkString(", "))

    return for {
      clients <- Future.sequence(partitionLeaders.zipWithIndex.map {tuple =>
        val hostAndPort = tuple._1.split(":")
        val partition = tuple._2
        AsyncLowLevelConsumer(topicName, partition, hostAndPort(0), hostAndPort(1).toInt)
      })
      partitionsLogSize <- Future.sequence(clients.map(client => client.offset))
      closeClients <- Future.sequence(clients.map(client => client.close))
    } yield partitionsLogSize
  }

  def getPartitionOffsets(topicName: String, zkClient: ZkClient): Future[Map[String, Seq[Long]]] = {
    Logger.debug("Getting partition offsets for topic " + topicName)
    return for {
      offsetsPartitionsNodes <- getZChildren(zkClient, "/consumers/*/offsets/" + topicName + "/*")
      partitionOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(2), p.name, new String(d.bytes).toLong)))))
      partitionOffsetsByConsumerGroup = partitionOffsets.groupBy(_._1).map(e1 => e1._1 -> e1._2.map(e2 => (e2._2, e2._3)))
      sortedPartitionOffsetsByConsumerGroup = partitionOffsetsByConsumerGroup.map(e => e._1 -> e._2.sortBy(p => p._1.toInt).map(p => p._2))
    } yield sortedPartitionOffsetsByConsumerGroup
  }

  def getTopics(zkClient: ZkClient): Future[Map[String, Seq[String]]] = {
    return for {
      allTopicNodes <- getZChildren(zkClient, "/brokers/topics/*")
      allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
      partitions <- getZChildren(zkClient, "/brokers/topics/*/partitions/*")
      topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
    } yield topics
  }

  def connectedZookeepers[A](block: (Zookeeper, ZkClient) => A): Seq[A] = {
    val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

    val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
      case Some(s: Map[_, _]) if connectedZks.size > 0 => s.asInstanceOf[Map[String, ZkClient]]
      case _ => Map()
    }

    zkConnections match {
      case _ if zkConnections.size > 0 => connectedZks.map(zk => block(zk, zkConnections.get(zk.name).get)).toSeq
      case _ => Seq.empty
    }

  }

  def getZChildren(zkClient: ZkClient, path: String): Future[Seq[ZNode]] = {
    val nodes = path.split('/').filter(_ != "").toSeq

    getZChildren(zkClient("/"), nodes)
  }

  def getZChildren(zNode: ZNode, path: Seq[String]): Future[Seq[ZNode]] = path match {

    case head +: tail if head == "*" => {

      val subtreesFuture = for {
        children <- twitterToScalaFuture(zNode.getChildren()).map(_.children).recover {
          case e: NoNodeException => Nil
        }
        subtrees <- Future.sequence(children.map(getZChildren(_, tail)))

      } yield subtrees

      subtreesFuture.map(_.flatten)
    }
    case head +: Nil => {
      twitterToScalaFuture(zNode(head).exists()).map(_ => Seq(zNode(head))).recover {
        case e: NoNodeException => Nil
      }
    }
    case head +: tail => getZChildren(zNode(head), tail)
    case Nil => Future(Seq(zNode))
  }

  def deleteZNode(zkClient: ZkClient, path: String): Future[ZNode] = {
    deleteZNode(zkClient(path))
  }

  def deleteZNode(zNode: ZNode): Future[ZNode] = {
    val deletePromise: Promise[ZNode] = Promise[ZNode]

    getZChildren(zNode, Seq("*")).map({ children =>
      val sequenceFuture = Future.sequence(children.map(n => deleteZNode(n)))

      sequenceFuture.onSuccess({ case children =>
        val delNode = twitterToScalaFuture(zNode.getData()).flatMap { d =>
          twitterToScalaFuture(zNode.delete(d.stat.getVersion))
        }

        delNode.onComplete(zNode => deletePromise complete zNode)
      })
      
      sequenceFuture.onFailure({ case t =>
        deletePromise failure t
      })
    })

    deletePromise.future
  }
}
