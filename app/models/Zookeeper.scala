package models

import org.squeryl.{Query, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.{CompositeKey, CompositeKey2, CompositeKey3}
import play.api.libs.json._
import models.Database._
import scala.collection.Iterable
import common.Registry
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

  //  def findById(address: String, port: Int, groupId: Long): Option[Server] = inTransaction {
  //    serversTable.lookup(address, port, groupId)
  //  }

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

  def update(zookeepers: Iterable[Zookeeper]) {
    inTransaction {
      zookeepersTable.update(zookeepers)
    }
  }
}

case class Zookeeper(@Column("name") val id: String, val host: String, val port: Int, val groupId: Long, val statusId: Long)
  extends KeyedEntity[String] {

  def name = id

  override def toString = "%s:%s".format(host, port)
}