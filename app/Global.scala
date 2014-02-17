
import akka.actor.{Props, ActorRef}
import core.Registry
import managers._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Session, SessionFactory}
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Concurrent
import play.api.{Application, GlobalSettings}
import Registry.PropertyConstants
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import router.{Message, Router}
import scala.Some
import scala.concurrent.duration._

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    //Database
    SessionFactory.concreteFactory = Some(() =>
      Session.create(DB.getConnection()(app), new H2Adapter)
    )

    val connectionManager = Akka.system.actorOf(Props(new ConnectionManager()))
    val databaseManager = Akka.system.actorOf(Props(new DatabaseManager()))
    val clientManager = Akka.system.actorOf(Props(new ClientManager()))
    val router = Akka.system.actorOf(Props(new Router(List(connectionManager, databaseManager, clientManager))))

    Registry.registerObject(PropertyConstants.BroadcastChannel, Concurrent.broadcast[String])
    Registry.registerObject(PropertyConstants.Router, router)

    for (zookeeper <- models.Zookeeper.findAll) {
      router ! Message.Connect(zookeeper)
    }
  }

  override def onStop(app: Application) {
    //    ConnectionManager.shutdownConnections()
  }

}