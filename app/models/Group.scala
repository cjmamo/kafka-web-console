package models

import org.squeryl.PrimitiveTypeMode._
import collection.Iterable
import org.squeryl.{Query, KeyedEntity}
import org.squeryl.dsl.OneToMany

object Group extends Enumeration  {

//  object Group extends Enumeration  {
//    type Group = Value

    val ALL = Value("ALL")
//  }

  import Database.groupsTable

  def findAll: Iterable[Group] = inTransaction {
    from(groupsTable) {
      group => select(group)
    }
  }

  def findByName(name: String) = inTransaction {
    from(groupsTable)(group => where(group.name === name) select (group)).headOption
  }
}

case class Group(val name: String) extends KeyedEntity[Long] {
  override val id = 0L

  lazy val servers: List[Server] = inTransaction {
    Database.groupToServers.left(this).toList
  }
}
