package kafka.consumer.async

import kafka.consumer.LowLevelConsumer
import scala.concurrent.future
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by isaacbanner on 12/16/14.
 */

class AsyncLowLevelConsumer(llc: LowLevelConsumer) {

  val consumer: LowLevelConsumer = llc

  def offset: Future[Long] = future {
    consumer.endingOffset()
  }

  def close = future {
    consumer.closeConsumers()
  }

}

object AsyncLowLevelConsumer {
  def apply(topic: String, partition: Int, seedBroker: String, port: Int, findLeader: Boolean = true) = future {
    val llc: LowLevelConsumer = new LowLevelConsumer(topic, partition, seedBroker, port, findLeader)
    new AsyncLowLevelConsumer(llc)
  }
}
