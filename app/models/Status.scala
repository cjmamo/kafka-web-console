package models

import org.squeryl.{KeyedEntity, Query}
import org.squeryl.PrimitiveTypeMode._
import scala.collection.Iterable

object Status extends Enumeration {
  type Status = Value

  val CONNECTED = Value("CONNECTED")
  val DISCONNECTED = Value("DISCONNECTED")

}

case class Status(val name: String) extends KeyedEntity[Long] {

  override val id = 0L

  lazy val servers: List[Server] = inTransaction {
    Database.statusToServers.left(this).toList
  }
}