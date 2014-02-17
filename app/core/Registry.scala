package core

import core.Registry.PropertyConstants.PropertyConstants

object Registry {

  object PropertyConstants extends Enumeration {
    type PropertyConstants = Value
    val SERVER_CONNECTIONS = Value("CONNECTED-SERVERS")
    val ROUTER = Value("ROUTER")
    val BROADCAST_CHANNEL = Value("BROADCAST-CHANNEL")
  }

  private var properties = Map[String, Any]()

  def lookupObject(propertyName: String): Option[Any] = {
    properties.get(propertyName)
  }

  def lookupObject(propertyName: PropertyConstants): Option[Any] = {
    this.lookupObject(propertyName.toString())
  }

  def registerObject(name: String, value: Any) {
    properties = properties ++ Map(name -> value)
  }

  def registerObject(name: PropertyConstants, value: Any) {
    this.registerObject(name.toString, value)
  }

}

