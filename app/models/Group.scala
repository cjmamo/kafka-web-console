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

import org.squeryl.PrimitiveTypeMode._
import collection.Iterable
import org.squeryl.{Query, KeyedEntity}
import org.squeryl.dsl.OneToMany

object Group extends Enumeration {

  val All = Value("ALL")
  val Development = Value("DEVELOPMENT")
  val Production = Value("PRODUCTION")
  val Staging = Value("STAGING")
  val Test = Value("TEST")

  import Database.groupsTable

  def findAll: Iterable[Group] = inTransaction {
    from(groupsTable)(group => select(group))
  }

  def findByName(name: String) = inTransaction {
    from(groupsTable)(group => where(group.name === name) select group).headOption
  }
}

case class Group(val name: String) extends KeyedEntity[Long] {
  override val id = 0L

  lazy val zookeepers: Seq[Zookeeper] = inTransaction {
    Database.groupToZookeepers.left(this).toList
  }
}
