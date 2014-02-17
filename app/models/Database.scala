package models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

object Database extends Schema {
  val serversTable = table[Server]("servers")
  val groupsTable = table[Group]("groups")
  val statusTable = table[Status]("status")

  val groupToServers = oneToManyRelation(groupsTable, serversTable).via((group, server) => group.id === server.groupId)
  val statusToServers = oneToManyRelation(statusTable, serversTable).via((status, server) => status.id === server.statusId)

//  on(this.serversTable) {
//    server =>
//      declare(
//        (server.address, server.port, server.groupId) is (unique)
//      )
//  }

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
