package com.flipcast.push.apns.service

import com.flipcast.Flipcast
import com.flipcast.push.apns.protocol.ApnsProtocol
import com.flipcast.push.common.FlipcastRequestConsumer
import com.flipcast.push.model.requests.{FlipcastPushRequest, RecordPushHistoryRequest}
import com.flipcast.push.protocol.FlipcastPushProtocol
import org.joda.time.DateTime



import scala.collection.JavaConverters._

/**
 * APNS consumer to send apns push messages
 */
class FlipcastApnsRequestConsumer extends FlipcastRequestConsumer[FlipcastPushRequest] with FlipcastPushProtocol with ApnsProtocol {

  override def configType() = "apns"

  override def init() {

  }

  def consume(request: FlipcastPushRequest) = {
    val service = ApnsServicePool.service(request.configName)
    val config = Flipcast.pushConfigurationProvider.config(request.configName).apns
    val ttl = request.ttl match {
      case Some(x) => x
      case _ =>  config.defaultExpiry
    }
    val expiry = ttl match {
      case x if x > 2419200 => new DateTime().plusSeconds(2419200) //Default it to 28 days if the expiry is set to more than 28 days
      case _ => new DateTime().plusSeconds(ttl)
    }
    service.push(request.registration_ids.asJavaCollection, request.data, expiry.toDate)
    //Record history for all successful devices
    request.registration_ids.par.foreach( r => {
      Flipcast.serviceRegistry.actor("pushMessageHistoryManager") ! RecordPushHistoryRequest[FlipcastPushRequest](request.configName, r, request)
    })
    true
  }
}
