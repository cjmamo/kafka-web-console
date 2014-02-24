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

package actors

import akka.actor.{Terminated, Actor}
import common.{Message, Registry}
import Registry.PropertyConstants
import play.api.libs.iteratee.{Concurrent, Enumerator}
import models.{Status, Zookeeper}

class DatabaseManager extends Actor {

  override def receive: Actor.Receive = {

    case connectNotification: Message.ConnectNotification => {
      val zk = connectNotification.zookeeper
      Zookeeper.upsert(zk)
    }
    case Terminated => {
      Zookeeper.update(Zookeeper.findAll.map(z => Zookeeper(z.id, z.host, z.port, z.groupId, Status.Disconnected.id)))
    }
  }

}
