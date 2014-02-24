package common

import scala.concurrent.{Future, Promise}
import com.twitter.util.{Throw, Return}
import com.twitter.zk.{ZNode, ZkClient}
import common.Registry.PropertyConstants
import models.Zookeeper
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.apache.zookeeper.KeeperException.{NotEmptyException, NodeExistsException, NoNodeException}

object Util {
  def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

  def connectedZookeepers[A](block: (Zookeeper, ZkClient) => A): List[A] = {
    val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

    val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
      case Some(s: Map[_, _]) if connectedZks.size > 0 => s.asInstanceOf[Map[String, ZkClient]]
      case _ => Map()
    }

    connectedZks.map(zk => block(zk, zkConnections.get(zk.id).get)).toList
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
        subtrees <- Future.sequence(children.map(getZChildren(_, tail)))

      } yield subtrees

      subtreesFuture.map(_.flatten)
    }
    case head :: Nil => {
      twitterToScalaFuture(zNode(head).exists()).map(_ => Seq(zNode(head))).recover {
        case e: NoNodeException => Nil
      }
    }
    case head :: tail => getZChildren(zNode(head), tail)
    case Nil => Future(Seq(zNode))
  }

  def deleteZNode(zkClient: ZkClient, path: String): Future[ZNode] = {
    deleteZNode(zkClient(path))
  }

  def deleteZNode(zNode: ZNode): Future[ZNode] = {
    val delNode = twitterToScalaFuture(zNode.getData()).flatMap { d =>
      twitterToScalaFuture(zNode.delete(d.stat.getVersion)).recover {
        case e: NotEmptyException => {
          for {
            children <- getZChildren(zNode, List("*"))
            delChildren <- Future.sequence(children.map(n => deleteZNode(n)))
          } yield deleteZNode(zNode)
        }
        case e: NoNodeException => Future(ZNode)
      }
    }

    //TODO: investigate why actual type is Future[Object]
    delNode.asInstanceOf[Future[ZNode]]
  }
}
