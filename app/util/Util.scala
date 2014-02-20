package util

import scala.concurrent.{Future, Promise}
import com.twitter.util.{Throw, Return}
import com.twitter.zk.{ZNode, ZkClient}
import core.Registry
import core.Registry.PropertyConstants
import models.Zookeeper
import scala.util.{Try, Success, Failure}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.apache.zookeeper.KeeperException.{Code, NoNodeException}

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

  def getZChildren(zkClient: ZkClient, path: String): Future[Seq[ZNode]] = {
    val nodes = path.split('/').filter(_ != "").toList
    getZChildren(zkClient("/"), nodes)
  }

  def getZChildren(zNode: ZNode, path: List[String]): Future[Seq[ZNode]] = path match {

    case head :: tail if head == "*" => {

      val subtreesFuture = for {
        children <- twitterToScalaFuture(zNode.getChildren()).map(_.children).recover {
          case e: NoNodeException => Nil
        }
        subtrees <- Future.sequence(children.map({
          getZChildren(_, tail)
        }))

      } yield subtrees

      subtreesFuture.map(_.flatten)
    }
    case head :: Nil => {
      twitterToScalaFuture(zNode(head).exists()).map(_ => Seq(zNode(head))).recover {
        case e: NoNodeException => Nil
      }
    }
    case head :: tail => {
      getZChildren(zNode(head), tail)
    }
    case Nil => {
      Future(Seq(zNode))
    }
  }

}
