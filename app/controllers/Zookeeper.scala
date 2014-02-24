package controllers

import play.api.mvc._
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import common.{Message, Registry}
import Registry.PropertyConstants
import akka.actor.ActorRef
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}

object Zookeeper extends Controller {

  val zookeeperForm = Forms.tuple(
    "name" -> Forms.text,
    "host" -> Forms.text,
    "port" -> Forms.number,
    "consumer_group" -> Forms.text,
    "group" -> Forms.text
  )

  def index(group: String) = Action {

    if (group.toUpperCase() == "ALL") {
      Ok(Json.toJson(models.Zookeeper.findAll.toList))
    }
    else {
      models.Group.findByName(group.toUpperCase) match {
        case Some(z) => Ok(Json.toJson(z.zookeepers))
        case _ => Ok(Json.toJson(List[String]()))
      }
    }
  }

  def create() = Action { implicit request =>
    val result = Form(zookeeperForm).bindFromRequest.fold(
      formFailure => BadRequest,
      formSuccess => {

        val name: String = formSuccess._1
        val host: String = formSuccess._2
        val port: Int = formSuccess._3
        val consumerGroup: String = formSuccess._4
        val group: String = formSuccess._5

        val zk = models.Zookeeper.insert(models.Zookeeper(name, host, port, models.Group.findByName(group.toUpperCase).get.id, models.Status.Disconnected.id, consumerGroup))

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

  def feed() = WebSocket.using[String] { implicit request =>

    val in = Iteratee.ignore[String]

    val out = Registry.lookupObject(PropertyConstants.BroadcastChannel) match {
      case Some(broadcastChannel: (Enumerator[_], Concurrent.Channel[_])) => broadcastChannel._1.asInstanceOf[Enumerator[String]]
      case _ => Enumerator.empty[String]
    }

    (in, out)
  }
}