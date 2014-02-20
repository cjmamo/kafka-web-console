package router

import models.{Status, Zookeeper}

object Message {

  case class Connect(val zookeeper: Zookeeper)
  case class ConnectNotification(val zookeeper: Zookeeper)
  case class StatusNotification(val status: Status)
  case class Terminate()

}
