package router

import models.{Status, Server}

object Message {

  case class Connect(val server: Server)
  case class ConnectNotification(val server: Server)
  case class StatusNotification(val status: Status)

}
