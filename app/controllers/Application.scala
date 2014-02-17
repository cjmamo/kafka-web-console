package controllers

import play.api._
import play.api.mvc._
import java.util.Properties
import java.util
import scala.collection.JavaConversions._
import kafka.utils.{Utils, ZkUtils}
import org.I0Itec.zkclient.ZkClient
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import java.lang.management.ManagementFactory
import play.api.libs.concurrent.Promise
import scala.concurrent.duration.DurationInt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import kafka.message.{MessageAndMetadata, ByteBufferMessageSet}
import kafka.serializer.StringDecoder
import scala.concurrent.Future
import kafka.consumer.ConsumerConfig
import kafka.javaapi.consumer.EventHandler
import play.api.data.{Form, Forms}
import scala.xml.MalformedAttributeException
import play.api.data.validation.Constraint

object Application extends Controller {

    def index = Action {
      Ok(views.html.index())
    }

//  def statusPage() = Action {
//    implicit request =>
//      Ok(views.html.statusPage(request))
//  }

  def statusFeed() = WebSocket.using[String] {
    implicit request =>

      val in = Iteratee.ignore[String]
      val (out, channel) = Concurrent.broadcast[String]
      val cb = (messageHolder: MessageAndMetadata[String, String]) => {
        channel.push(messageHolder.message)
      }

      val topicCountMap = new util.HashMap[EventHandler[String, String], Integer]()
      topicCountMap.put(new EventHandler("ossandme", cb), 1)
      topicCountMap.put(new EventHandler("ossandme2", cb), 1)
      topicCountMap.put(new EventHandler("ossandme3", cb), 2)

//      val consumer = new kafka.javaapi.consumer.ZookeeperEventConsumerConnector(createConsumerConfig("localhost:2181", "0"), true)
//      val consumerMap = consumer.createMessageStreams(topicCountMap, new StringDecoder(), new StringDecoder())

      //      val consumer = new kafka.javaapi.consumer.ZookeeperEventConsumerConnector(createConsumerConfig("localhost:2181", "0"), cb)
      //      val topicCountMap = new util.HashMap[String, Integer]()
      //      topicCountMap.put("ossandme", new Integer(1))
      //      val consumerMap = consumer.subscribe(topicCountMap)

      //      val zkClient = new ZkClient("localhost:2181");
      //      val topics = ZkUtils.getAllTopics(zkClient);
      //      val out = Enumerator.repeatM {
      //        Promise.timeout(getLoadAverage, 3 seconds)
      //      }

      (in, out)
  }

}