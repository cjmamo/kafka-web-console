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

import akka.actor.{Terminated, Props, ActorRef}
import common.{Message, Registry}
import actors._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Session, SessionFactory}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Concurrent
import play.api.{Application, GlobalSettings}
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
    val router = Akka.system.actorSelection("akka://application/user/router")
    router ! Terminated
  }

  private def initiateDb(app: Application) {
    SessionFactory.concreteFactory = Some(() =>
      Session.create(DB.getConnection()(app), new H2Adapter)
    )
  }

  private def initiateActors() {
    Akka.system.actorOf(Props(new Router()), "router")
    Akka.system.actorOf(Props(new ConnectionManager()))
    Akka.system.actorOf(Props(new ClientNotificationManager()))
  }

}