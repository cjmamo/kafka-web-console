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

object Group extends Controller {

  def index() = Action {
    val zookeepers = models.Group.findByName("ALL").get.zookeepers

    Ok(Json.toJson(zookeepers))
  }

}
