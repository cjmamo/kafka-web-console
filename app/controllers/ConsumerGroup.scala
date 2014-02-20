package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import util.Util._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Failure
import com.twitter.zk.ZNode
import scala.Some

object ConsumerGroup extends Controller {

  def show(zookeeper: String, topic: String) = Action.async {

    connectedZookeepers((z, c) => (z, c)) match {
      case Some(l) => {
        val (_, zkClient) = l.filter(_._1.name == zookeeper).head

        val partitionsAndOffsetsByConsumerFuture = for {
          ownedTopicNodes <- getZChildren(zkClient, "/consumers/*/owners/" + topic)
          allConsumers = ownedTopicNodes.map(n => (n.path.split("/").filter(_ != "")(1), Nil)).toMap
          offsetsPartitionsNodes <- getZChildren(zkClient, "/consumers/*/offsets/" + topic + "/*")
          consumersAndPartitionsAndOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(2), p.name, new String(d.bytes))))))
          partitionsAndOffsetsByConsumer = consumersAndPartitionsAndOffsets.groupBy(_._1).map(s => (s._1 -> s._2.map(t => (t._2, t._3))))
        } yield allConsumers ++ partitionsAndOffsetsByConsumer

        partitionsAndOffsetsByConsumerFuture.map(poc => Ok(views.html.consumer.show(poc)))

      }
      case _ => Future(Ok(views.html.consumer.show()))
    }
  }
}
