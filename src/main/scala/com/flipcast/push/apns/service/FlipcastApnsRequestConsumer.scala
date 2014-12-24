package com.flipcast.push.apns.service

import java.util.{Calendar, Date}

import com.flipcast.Flipcast
import com.flipcast.push.apns.protocol.ApnsProtocol
import com.flipcast.push.common.FlipcastRequestConsumer
import com.flipcast.push.model.requests.{FlipcastPushRequest, RecordPushHistoryRequest}
import com.flipcast.push.protocol.FlipcastPushProtocol

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
    val c = Calendar.getInstance()
    c.setTime(new Date())
    c.add(Calendar.MINUTE, ttl)
    service.push(request.registration_ids.asJavaCollection, request.data, c.getTime)
    //Record history for all successful devices
    request.registration_ids.par.foreach( r => {
      Flipcast.serviceRegistry.actor("pushMessageHistoryManager") ! RecordPushHistoryRequest[FlipcastPushRequest](request.configName, r, request)
    })
    true
  }
}
