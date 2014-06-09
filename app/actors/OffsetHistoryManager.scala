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

package actors

import akka.actor.Actor
import akka.actor.Actor.Receive
import models.{Zookeeper, OffsetPoint, OffsetHistory, Status}
import common.Util._
import scala.Some
import common.{Message, Registry}
import common.Registry.PropertyConstants
import com.twitter.zk.ZkClient
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.Logger
import java.sql.Timestamp
import java.util.Date
import org.apache.zookeeper.ZooKeeper


class OffsetHistoryManager extends Actor {

  override def preStart() {
    self ! Message.SaveOffsetPoints
  }

  override def receive: Receive = {
    case Message.SaveOffsetPoints => {
      connectedZookeepers { (zk, zkClient) =>

        for {
          topics <- getTopics(zkClient)
          topic = topics.map { t =>
            for {
              partitionLeaders <- getPartitionLeaders(t._1, zkClient)
              partitionsLogSize <- getPartitionsLogSize(t._1, partitionLeaders)
              partitionOffsets <- getPartitionOffsets(t._1, zkClient)
            } yield saveOffsetPoint(partitionOffsets, getOffsetHistory(zk, t), partitionsLogSize)
          }
        } yield None
      }

      Akka.system.scheduler.scheduleOnce(Duration.create(60, TimeUnit.SECONDS), self, Message.SaveOffsetPoints)
    }
    case _ =>
  }

  private def getOffsetHistory(zk: Zookeeper, topic: (String, Seq[String])): OffsetHistory = {
    OffsetHistory.findByZookeeperIdAndTopic(zk.id, topic._1) match {
      case None => OffsetHistory.insert(OffsetHistory(zk.id, topic._1))
      case Some(oH) => oH
    }
  }

  private def saveOffsetPoint(partitionOffsets: Map[String, Seq[Long]], offsetHistory: OffsetHistory, partitionsLogSize: Seq[Long]) {
    val timestamp = new Timestamp(new Date().getTime)
    for (e <- partitionOffsets) {
      for ((p, i) <- e._2.zipWithIndex) {
        OffsetPoint.insert(OffsetPoint(e._1, timestamp, offsetHistory.id, i, p, partitionsLogSize(i)))
      }

    }
  }
}
