package util

import scala.concurrent.{Promise, Future}
import com.twitter.util.{Throw, Return}
import com.twitter.zk.ZkClient
import core.Registry
import core.Registry.PropertyConstants
import models.Zookeeper

object Util {
  def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

  def connectedZookeepers[A](block: (Zookeeper, ZkClient) => A): Option[List[A]] = {
    val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

    val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
      case c: Some[Map[String, ZkClient]] if connectedZks.size > 0 => c.get
    }

    if (connectedZks.size > 0) {
      Option(connectedZks.map(zk => block(zk, zkConnections.get(zk.id).get)).toList)
    }
    else {
      None
    }
  }

}
