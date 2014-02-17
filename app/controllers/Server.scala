package controllers

import play.api.mvc._
import play.api.mvc.Results._
import java.util.Properties
import kafka.consumer.ConsumerConfig
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import kafka.javaapi.consumer.AsyncConsumerConnector
import core.Registry
import Registry.PropertyConstants
import akka.actor.ActorRef
import router.Message


object Server extends Controller {

  val zookeeperForm = Forms.tuple(
    "address" -> Forms.text,
    "port" -> Forms.number
  )

  def index(group: String) = Action {
    implicit request =>

      val servers = models.Group.findByName(group).get.servers

      render {
        case Accepts.Html() => {
          Ok(views.html.server.index(Form(zookeeperForm)))
        }
        case Accepts.Json() => {
          Ok(Json.toJson(servers))
        }
      }

  }

  def create() = Action {
    implicit request =>
      val result = Form(zookeeperForm).bindFromRequest.fold(
        formFailure => {
          BadRequest
        },
        formSuccess => {

          val address: String = formSuccess._1
          val port: Int = formSuccess._2

          val server = models.Server.insert(models.Server(address, port, models.Group.findByName("ALL").get.id, models.Status.DISCONNECTED.id))

          try {
            Registry.lookupObject(PropertyConstants.ROUTER) match {
              case Some(router: ActorRef) => router ! Message.Connect(server)
              case _ =>
            }

            Ok
          }
          catch {
            case e: Exception => BadRequest
          }

        }

      )

      result
  }
}
