package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import util.Util._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Consumer extends Controller {

  def show(zookeeper: String, topic: String) = Action.async {

    connectedZookeepers((z, c) => (z, c)) match {
      case Some(l) => {
        val (_, zkClient) = l.filter(_._1.name == zookeeper).head
        val topicConsumersFuture = for {
          consumers <- twitterToScalaFuture(zkClient("/consumers").getChildren().map(_.children))
          consumersAndTopics <- Future.sequence(consumers.map { c =>
            twitterToScalaFuture(zkClient("/consumers/" + c.name + "/owners").getChildren().map(t => (c.name, t.children)))
          })
        } yield consumersAndTopics.filter(_._2.filter(_.name == topic).size > 0).map(_._1)

        topicConsumersFuture.map(topicConsumers => Ok(views.html.consumer.show(topicConsumers)))
      }
      case _ => Future(Ok(views.html.consumer.show()))
    }

  }

}
