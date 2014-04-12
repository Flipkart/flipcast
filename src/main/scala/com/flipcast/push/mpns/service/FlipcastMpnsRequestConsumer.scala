package com.flipcast.push.mpns.service

import com.flipcast.push.common.FlipcastRequestConsumer
import com.flipcast.push.protocol.FlipcastPushProtocol
import com.flipcast.push.gcm.protocol.GcmProtocol
import spray.json.JsonParser
import com.flipcast.push.model.requests.FlipcastPushRequest
import com.flipcast.Flipcast

/**
 * Message Consumer for MPNS requests
 * Will use the MPNS endpoint to send the messages using spray-httpclient
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastMpnsRequestConsumer extends FlipcastRequestConsumer with FlipcastPushProtocol with GcmProtocol {

  override def configType() = "gcm"

  override def init() {

  }

  override def consume(message: String) = {
    val request = JsonParser(message).convertTo[FlipcastPushRequest]
    val config = Flipcast.pushConfigurationProvider.config(request.configName).gcm
    val ttl = request.ttl match {
      case Some(x) => x
      case _ => config.defaultExpiry
    }

    true
  }

}
