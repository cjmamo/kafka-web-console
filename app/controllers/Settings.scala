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

package controllers

import play.api.mvc.{Controller, Action}
import play.api.libs.json._
import play.api.data.{Forms, Form}
import play.api.libs.concurrent.Akka
import common.Message
import play.api.data.Forms._
import scala.Some
import models.Setting
import play.api.Play.current
import play.Logger

object Settings extends Controller {

  def update() = Action { request =>
    request.body.asJson match {
      case Some(JsArray(settings)) => {
        updateSettings(settings)
        Ok
      }
      case _ => BadRequest
    }
  }

  def index() = Action {
    Ok(Json.toJson(Setting.findAll))
  }

  def updateSettings(settings : Seq[JsValue]) {
    settings.map { s =>
      Setting.update(Setting(s.\("key").as[String], s.\("value").as[String]))
      Akka.system.actorSelection("akka://application/user/router") ! Message.SettingsUpdateNotification
    }
  }

}
