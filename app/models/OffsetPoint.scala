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
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import models.Database._
import org.squeryl.dsl.CompositeKey2
import java.sql.Timestamp
import play.api.libs.json.{Json, Writes}

object OffsetPoint {

  import Database.offsetPointsTable

  implicit object OffsetPointWrites extends Writes[OffsetPoint] {
    def writes(offsetPoint: OffsetPoint) = {

      Json.obj(
        "consumerGroup" -> offsetPoint.consumerGroup,
        "timestamp" -> offsetPoint.timestamp,
        "partition" -> offsetPoint.partition,
        "offset" -> offsetPoint.offset,
        "logSize" -> offsetPoint.logSize
      )
    }
  }

  def findByOffsetHistoryIdAndConsumerGroup(offsetHistoryId: Long, consumerGroup: String): Seq[OffsetPoint] = inTransaction {
    from(offsetPointsTable)(oP => where(oP.offsetHistoryId === offsetHistoryId and oP.consumerGroup === consumerGroup) select (oP)).toList
  }

  def update(offsetPoint: OffsetPoint) = inTransaction {
    offsetPointsTable.update(offsetPoint)
  }

  def insert(offsetPoint: OffsetPoint): OffsetPoint = inTransaction {
    offsetPointsTable.insert(offsetPoint)
  }
}

case class OffsetPoint(consumerGroup: String, timestamp: Timestamp, offsetHistoryId: Long, partition: Int, offset: Long, logSize: Long) extends KeyedEntity[Long] {
  override val id: Long = 0
}