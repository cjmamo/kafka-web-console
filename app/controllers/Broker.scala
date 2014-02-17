package controllers

import play.api.mvc.{Action, Controller}
import com.twitter.zk.ZkClient
import core.Registry
import core.Registry.PropertyConstants
import scala.concurrent.{Promise, Future}
import com.twitter.util.{Throw, Return}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.parsing.json.JSON

object Broker extends Controller {

  def index = Action.async {
    request =>

      val servers = models.Server.findByStatusId(models.Status.CONNECTED.id)

      val serverConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.SERVER_CONNECTIONS) match {
        case serverConnections: Some[Map[String, ZkClient]] => serverConnections.get
      }

      val connectedServers = servers.filter(server => serverConnections.contains(server.toString))

      val brokers = connectedServers.map(server => {

        val zkClient = serverConnections.get(server.toString).get
        val zNode = zkClient.apply("/brokers/ids")
        twitterToScalaFuture(zNode.getChildren.apply().map(brokerIdsChild => brokerIdsChild.children.map(brokerId => {
          twitterToScalaFuture(zkClient.apply(brokerId.path).getData.apply().map(broker => {
            scala.util.parsing.json.JSON.parseFull(new String(broker.bytes)).get.asInstanceOf[Map[String, Any]]
          }))
        })))
      }).toList

      if (brokers.size > 0) {


        Future.reduce(brokers)((topic, allTopics) => {
          topic ++ allTopics
        }).flatMap(topic => {
          Future.sequence(topic).map(t => {
            Ok(views.html.broker.index(t))
          })
        })
      }
      else {
        Future(Ok(views.html.topic.index()))
      }
  }

  private def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

}
