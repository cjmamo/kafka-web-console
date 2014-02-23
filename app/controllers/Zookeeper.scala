package controllers

import play.api.mvc._
import play.api.mvc.Results._
import java.util.Properties
import kafka.consumer.ConsumerConfig
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import kafka.javaapi.consumer.AsyncConsumerConnector
import core.{Message, Registry}
import Registry.PropertyConstants
import akka.actor.ActorRef


object Zookeeper extends Controller {

  val zookeeperForm = Forms.tuple(
    "name" -> Forms.text,
    "host" -> Forms.text,
    "port" -> Forms.number
  )

  def index(group: String) = Action { implicit request =>
    val zookeepers = models.Group.findByName(group).get.zookeepers
    Ok(Json.toJson(zookeepers))
  }

  def create() = Action {
    implicit request =>
      val result = Form(zookeeperForm).bindFromRequest.fold(
        formFailure => {
          BadRequest
        },
        formSuccess => {

          val name: String = formSuccess._1
          val host: String = formSuccess._2
          val port: Int = formSuccess._3

          val zk = models.Zookeeper.insert(models.Zookeeper(name, host, port, models.Group.findByName("ALL").get.id, models.Status.Disconnected.id))

          try {
            Registry.lookupObject(PropertyConstants.Router) match {
              case Some(router: ActorRef) => router ! Message.Connect(zk)
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
