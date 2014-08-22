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

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import common.Util._
import play.api.libs.json.{Writes, Json}
import scala.Some
import models.Zookeeper
import com.twitter.zk.ZkClient

object Broker extends Controller {

  implicit object BrokerWrites extends Writes[Seq[(String, String, Map[String, Any])]] {
    def writes(l: Seq[(String, String, Map[String, Any])]) = {
      val brokers = l.map { i =>

        val fields = i._3.map { kv =>
          kv._2 match {
            case v: Double => (kv._1, v.toInt.toString)
            case _ => (kv._1, kv._2.toString())
          }
        }

        fields + ("zookeeper" -> i._1) + ("id" -> i._2)
      }
      Json.toJson(brokers)
    }
  }

  def index = Action.async {
    val brokers = connectedZookeepers { (zk, zkClient) => getBrokers(zk, zkClient)}
    Future.sequence(brokers).map(l => Ok(Json.toJson(l.flatten)))
  }

  private def getBrokers(zk: Zookeeper, zkClient: ZkClient): Future[Seq[(String, String, Map[String, Any])]] = {
    return for {
      brokerIds <- getZChildren(zkClient, "/brokers/ids/*")
      brokers <- Future.sequence(brokerIds.map(brokerId => twitterToScalaFuture(brokerId.getData())))
    } yield brokers.map(b => (zk.name, b.toString.substring(b.toString.lastIndexOf("/")+1, b.toString.length()-1), scala.util.parsing.json.JSON.parseFull(new String(b.bytes)).get.asInstanceOf[Map[String, Any]]))
  }

}
