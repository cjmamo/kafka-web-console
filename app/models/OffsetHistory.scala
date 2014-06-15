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

package models

import org.squeryl.annotations._
import org.squeryl.{Session, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import models.Database._

object OffsetHistory {

  def truncate() = inTransaction {
    offsetHistoryTable.deleteWhere(r => 1 === 1)
  }

  def delete(offsetHistory: OffsetHistory) = inTransaction {
    offsetHistoryTable.deleteWhere(oH => oH.id === offsetHistory.id)
  }

  def findByZookeeperIdAndTopic(zookeeperId: Long, topic: String): Option[OffsetHistory] = inTransaction {
    from(offsetHistoryTable)(oH => where(oH.zookeeperId === zookeeperId and oH.topic === topic) select (oH)).headOption
  }

  def findByZookeeperId(zookeeperId: Long): Seq[OffsetHistory] = inTransaction {
    from(offsetHistoryTable)(oH => where(oH.zookeeperId === zookeeperId) select (oH)).toList
  }

  def insert(offsetHistory: OffsetHistory): OffsetHistory = inTransaction {
    offsetHistoryTable.insert(offsetHistory)
  }
}

case class OffsetHistory(zookeeperId: Long, topic: String) extends KeyedEntity[Long] {
  override val id: Long = 0

  lazy val offsetPoints: Seq[OffsetPoint] = inTransaction {
    Database.offsetHistoryToOffsetPoints.left(this).toList
  }
}