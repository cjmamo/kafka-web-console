/**
 * Copyright (C) 2014 the original author or authors and Enernoc Inc.
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Author: Isaac Banner | ibanner56
 */

package kafka.consumer.async

import kafka.consumer.LowLevelConsumer
import scala.concurrent.future
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class AsyncLowLevelConsumer(consumer: LowLevelConsumer) {

  def offset: Future[Long] = future {
    consumer.endingOffset()
  }

  def close = future {
    consumer.closeConsumers()
  }

}

object AsyncLowLevelConsumer {
  def apply(topic: String, partition: Int, seedBroker: String, port: Int) = future {
    val llc: LowLevelConsumer = new LowLevelConsumer(topic, partition, seedBroker, port)
    new AsyncLowLevelConsumer(llc)
  }
}