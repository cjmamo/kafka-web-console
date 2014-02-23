package models

import org.squeryl.{KeyedEntity, Query}
import org.squeryl.PrimitiveTypeMode._
import scala.collection.Iterable

object Status extends Enumeration {
  type Status = Value

  val Disconnected = Value("DISCONNECTED")
  val Connected = Value("CONNECTED")

}

case class Status(val name: String) extends KeyedEntity[Long] {

  override val id = 0L

  lazy val zookeepers: List[Zookeeper] = inTransaction {
    Database.statusToZookeepers.left(this).toList
  }
}