package controllers

import play.api.mvc.{Controller, Action}
import play.api.libs.json._

object Group extends Controller {

  def index() = Action {
    val zookeepers = models.Group.findByName("ALL").get.zookeepers

    Ok(Json.toJson(zookeepers))
  }

}
