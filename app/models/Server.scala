package models

import org.squeryl.{Query, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.{CompositeKey2, CompositeKey3}
import play.api.libs.json._
import models.Database._
import scala.collection.Iterable
import core.Registry

object Server {

  import Database.serversTable

//  implicit val serverFormat = Json.format[Server]
  implicit object ServerWrites extends Writes[Server] {
    def writes(server: Server) = {

      Json.obj(
        "address" -> server.address,
        "port" -> server.port,
        "group" -> Group.apply(server.groupId.toInt).toString,
        "status" -> Status.apply(server.statusId.toInt).toString
      )
    }
  }

  def findAll: Iterable[Server] = inTransaction {
    from(serversTable) {
      server => select(server)
    }.toList
  }

  def findByStatusId(statusId: Long): Iterable[Server] = inTransaction {
    from(serversTable)(server => where(server.statusId === statusId) select (server)).toList
  }

//  def findById(address: String, port: Int, groupId: Long): Option[Server] = inTransaction {
//    serversTable.lookup(address, port, groupId)
//  }

  def upsert(server: Server) = inTransaction {
    val serverCount = from(serversTable)(s => where ((server.address === s.address) and
                                                     (server.port === s.port)) select(s)).toList.size
    serverCount match {
      case 1 => this.update(server)
      case _ if serverCount < 1 => this.insert(server)
      case _ =>
    }

  }

  def insert(server: Server) = inTransaction {
    serversTable.insert(server)
  }

  def update(server: Server) = inTransaction {
    serversTable.update(server)
  }
}

case class Server(val address: String, val port: Int, val groupId: Long, val statusId: Long)
  extends KeyedEntity[CompositeKey2[String, Int]] {

  def id = compositeKey(address, port)

  override def toString = "%s:%s".format(address, port)
}