package com.flipcast.push.common

import akka.actor.Actor
import akka.event.slf4j.Logger
import com.flipcast.push.model.SidelinedMessage
import com.flipcast.push.protocol.FlipcastPushProtocol

/**
 * Sideline persistence for abandoned/aborted messages
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastSidelineConsumer extends Actor with FlipcastPushProtocol {

  lazy val log = Logger("sideline")

  override def preStart() {
    log.info("Starting sideline message consumer: " +self.path)
  }

  override def postStop(): Unit = {
    log.info("Stopping sideline message consumer: " +self.path)
  }

  def receive = {
    case request: SidelinedMessage =>
      DeviceDataSourceManager.dataSource(request.config)
        .sidelineMessage(request)
  }

}
