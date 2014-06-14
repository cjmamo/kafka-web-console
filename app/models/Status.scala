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

import org.squeryl.{KeyedEntity, Query}
import org.squeryl.PrimitiveTypeMode._
import scala.collection.Iterable

object Status extends Enumeration {
  type Status = Value

  val Connecting = Value("CONNECTING")
  val Connected = Value("CONNECTED")
  val Disconnected = Value("DISCONNECTED")
  val Deleted = Value("Deleted")

}

case class Status(name: String) extends KeyedEntity[Long] {

  override val id = 0L

  lazy val zookeepers: Seq[Zookeeper] = inTransaction {
    Database.statusToZookeepers.left(this).toSeq
  }
}