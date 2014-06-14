/*
 * Copyright 2014 Claude Mamo
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
 */

package actors

import akka.actor.{ActorRef, Actor}
import org.quartz._
import java.util.Properties
import org.quartz.impl.StdSchedulerFactory
import models.{OffsetHistory, OffsetPoint, Setting}
import common.Message

private class Executor() extends Job {
  def execute(ctx: JobExecutionContext) {
    val actor = ctx.getJobDetail.getJobDataMap().get("actor").asInstanceOf[ActorRef]
    actor ! Message.Purge
  }
}

class Purger extends Actor {

  private val JobKey = "purge"
  private[this] val props = new Properties()

  props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
  props.setProperty("org.quartz.threadPool.threadCount", "1")
  props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
  props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")

  val scheduler = new StdSchedulerFactory(props).getScheduler

  override def preStart() {
    scheduler.start()
    schedule()
  }

  override def postStop() {
    scheduler.shutdown()
  }

  override def receive: Receive = {
    case Message.SettingsUpdateNotification => {
      scheduler.deleteJob(new JobKey(JobKey))
      schedule()
    }
    case Message.Purge => {
      OffsetPoint.truncate()
      OffsetHistory.truncate()
    }
  }

  private def schedule() {
    val jdm = new JobDataMap()
    jdm.put("actor", self)
    val job = JobBuilder.newJob(classOf[Executor]).withIdentity(JobKey).usingJobData(jdm).build()
    scheduler.scheduleJob(job, TriggerBuilder.newTrigger().startNow().forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(Setting.findByKey("PURGE_SCHEDULE").get.value)).build())
  }
}
