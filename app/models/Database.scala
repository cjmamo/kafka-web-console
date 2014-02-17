package models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

object Database extends Schema {
  val zookeepersTable = table[Zookeeper]("zookeepers")
  val groupsTable = table[Group]("groups")
  val statusTable = table[Status]("status")

  val groupToZookeepers = oneToManyRelation(groupsTable, zookeepersTable).via((group, zk) => group.id === zk.groupId)
  val statusToZookeepers = oneToManyRelation(statusTable, zookeepersTable).via((status, zk) => status.id === zk.statusId)

  on(this.groupsTable) {
    group =>
      declare(
        group.id is (autoIncremented),
        group.name is (unique)
      )
  }

  on(this.statusTable) {
    status =>
      declare(
        status.id is (autoIncremented),
        status.name is (unique)
      )
  }

}
