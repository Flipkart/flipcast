package com.flipcast.push.common

import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Put, Remove}
import akka.event.slf4j.Logger
import com.flipcast.push.model.SidelinedMessage
import com.flipcast.push.protocol.FlipcastPushProtocol

/**
 * Sideline persistence for abandoned/aborted messages
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastSidelineConsumer extends Actor with FlipcastPushProtocol {

  val mediator = DistributedPubSubExtension(context.system).mediator

  lazy val log = Logger("sideline")

  override def preStart() {
    mediator ! Put(self)
    log.info("Starting sideline message consumer: " +self.path)
  }

  override def postStop(): Unit = {
    mediator ! Remove(self.path.toString)
    log.info("Stopping sideline message consumer: " +self.path)
  }

  def receive = {
    case request: SidelinedMessage =>
      DeviceDataSourceManager.dataSource(request.config)
        .sidelineMessage(request)
  }

}
