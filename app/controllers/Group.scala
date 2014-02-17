package controllers

import play.api.mvc.{Controller, Action}
import play.api.libs.json._

object Group extends Controller {

  def index() = Action {
    val servers = models.Group.findByName("ALL").get.servers

    Ok(Json.toJson(servers))
  }

}
