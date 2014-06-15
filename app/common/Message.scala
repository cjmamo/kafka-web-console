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

package common

import models.{Status, Zookeeper}

object Message {

  case class Connect(zookeeper: Zookeeper)

  case class Disconnect(zookeeper: Zookeeper)

  case class ConnectNotification(zookeeper: Zookeeper)

  case class StatusNotification(status: Status)

  case class Terminate()

  case class FetchOffsets()

  case class SettingsUpdateNotification()

  case class Purge()

  object ConnectNotification {
    def apply(zk: Zookeeper, status: Status.Value): ConnectNotification = {
      ConnectNotification(Zookeeper(zk.name, zk.host, zk.port, zk.groupId, status.id, zk.chroot, zk.id))
    }
  }

}
