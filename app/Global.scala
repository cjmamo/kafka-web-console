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

import akka.actor.{Terminated, Props}
import common.Registry
import actors._
import models.{Setting, OffsetHistory}
import org.squeryl.adapters._
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.{Session, SessionFactory}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Concurrent
import play.api.{Logger, Application, GlobalSettings}
import Registry.PropertyConstants
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import scala.Some

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Registry.registerObject(PropertyConstants.BroadcastChannel, Concurrent.broadcast[String])
    initiateDb(app)
    initiateActors()
  }

  override def onStop(app: Application) {
    Akka.system.actorSelection("akka://application/user/router") ! Terminated
  }

  private def initiateDb(app: Application) {
    SessionFactory.concreteFactory = app.configuration.getString("db.default.driver") match {
      case Some("org.h2.Driver") => Some(() => getSession(new H2Adapter, app))
      case Some("org.postgresql.Driver") => Some(() => getSession(new PostgreSqlAdapter, app))
      case Some("oracle.jdbc.OracleDriver") => Some(() => getSession(new OracleAdapter, app))
      case Some("com.ibm.db2.jcc.DB2Driver") => Some(() => getSession(new DB2Adapter, app))
      case Some("com.mysql.jdbc.Driver") => Some(() => getSession(new MySQLAdapter, app))
      case Some("org.apache.derby.jdbc.EmbeddedDriver") => Some(() => getSession(new DerbyAdapter, app))
      case Some("com.microsoft.sqlserver.jdbc.SQLServerDriver") => Some(() => getSession(new MSSQLServer, app))
      case _ => sys.error("Database driver must be either org.h2.Driver, org.postgresql.Driver, oracle.jdbc.OracleDriver, com.ibm.db2.jcc.DB2Driver, com.mysql.jdbc.Driver, org.apache.derby.jdbc.EmbeddedDriver or com.microsoft.sqlserver.jdbc.SQLServerDriver")
    }
  }

  private def getSession(adapter: DatabaseAdapter, app: Application) = Session.create(DB.getConnection()(app), adapter)

  private def initiateActors() {
    Akka.system.actorOf(Props(new Router()), "router")
    Akka.system.actorOf(Props(new ConnectionManager()))
    Akka.system.actorOf(Props(new ClientNotificationManager()))
    Akka.system.actorOf(Props(new OffsetHistoryManager()))
  }

}