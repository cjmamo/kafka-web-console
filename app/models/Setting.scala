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

import org.squeryl.{Session, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import models.Database._
import org.squeryl.annotations.Column
import play.api.libs.json.{Json, Writes}

object Setting extends Enumeration {
  import Database.settingsTable

  val PurgeSchedule = Value("PURGE_SCHEDULE")
  val OffsetFetchInterval = Value("OFFSET_FETCH_INTERVAL")

  implicit object SettingWrites extends Writes[Setting] {
    def writes(setting: Setting) = {
      Json.obj(
        "key" -> setting.key,
        "value" -> setting.value
      )
    }
  }

  def findAll: Seq[Setting] = inTransaction {
    from(settingsTable) {
      s => select(s)
    }.toList
  }

  def findByKey(key: String): Option[Setting] = inTransaction {
    from(settingsTable)(s => where(s.key === key) select (s)).headOption
  }

  def update(setting: Setting) = inTransaction {
    settingsTable.update(setting)
  }
}

case class Setting(@Column("key_") id: String, value: String) extends KeyedEntity[String] {
  def key = id
}
