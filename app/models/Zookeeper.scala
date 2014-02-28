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

import org.squeryl.{Query, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.{CompositeKey, CompositeKey2, CompositeKey3}
import play.api.libs.json._
import models.Database._
import scala.collection.Iterable
import org.squeryl.annotations.Column

object Zookeeper {

  import Database.zookeepersTable

  implicit object ZookeeperWrites extends Writes[Zookeeper] {
    def writes(zookeeper: Zookeeper) = {

      Json.obj(
        "name" -> zookeeper.name,
        "host" -> zookeeper.host,
        "port" -> zookeeper.port,
        "group" -> Group.apply(zookeeper.groupId.toInt).toString,
        "status" -> Status.apply(zookeeper.statusId.toInt).toString
      )
    }
  }

  def findAll: Iterable[Zookeeper] = inTransaction {
    from(zookeepersTable) {
      zk => select(zk)
    }.toList
  }

  def findByStatusId(statusId: Long): Iterable[Zookeeper] = inTransaction {
    from(zookeepersTable)(zk => where(zk.statusId === statusId) select (zk)).toList
  }

  def findById(name: String): Option[Zookeeper] = inTransaction {
    zookeepersTable.lookup(name)
  }

  def upsert(zookeeper: Zookeeper) = inTransaction {
    val zkCount = from(zookeepersTable)(z => where(zookeeper.name === z.name) select (z)).toList.size
    zkCount match {
      case 1 => this.update(zookeeper)
      case _ if zkCount < 1 => this.insert(zookeeper)
      case _ =>
    }
  }

  def insert(zookeeper: Zookeeper) = inTransaction {
    zookeepersTable.insert(zookeeper)
  }

  def update(zookeeper: Zookeeper) = inTransaction {
    zookeepersTable.update(zookeeper)
  }

  def delete(zookeeper: Zookeeper) = inTransaction {
    zookeepersTable.delete(zookeeper.name)
  }

  def update(zookeepers: Iterable[Zookeeper]) {
    inTransaction {
      zookeepersTable.update(zookeepers)
    }
  }
}

case class Zookeeper(@Column("name") id: String, host: String, port: Int, groupId: Long, statusId: Long)
  extends KeyedEntity[String] {

  def name = id

  override def toString = "%s:%s".format(host, port)
}