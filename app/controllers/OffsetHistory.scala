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

import play.api.libs.json.Json
import play.api.mvc._
import models.OffsetPoint

object OffsetHistory extends Controller {

  def show(consumerGroup: String, topic: String, zookeeper: String) = Action {

    models.Zookeeper.findByName(zookeeper) match {
      case Some(zk) => {
        models.OffsetHistory.findByZookeeperIdAndTopic(zk.id, topic) match {
          case Some(oH) => Ok(Json.toJson(OffsetPoint.findByOffsetHistoryIdAndConsumerGroup(oH.id, consumerGroup)))
          case _ => Ok(Json.toJson(Seq[String]()))
        }
      }
      case _ => Ok(Json.toJson(Seq[String]()))
    }
  }

}
