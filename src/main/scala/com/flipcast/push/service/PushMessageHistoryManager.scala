package com.flipcast.push.service

import akka.actor.Actor
import akka.event.slf4j.Logger
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.model.requests.{FlipcastPushRequest, RecordPushHistoryRequest}
import com.flipcast.push.protocol.FlipcastPushProtocol
import spray.json._
/**
 * Keep track of push history
 */
class PushMessageHistoryManager extends Actor with FlipcastPushProtocol {

  val log = Logger("DeviceHouseKeepingManager")

  def receive = {
    case request: RecordPushHistoryRequest[_] =>
      DeviceDataSourceManager.dataSource(request.configName).recordHistory(request.configName, request.key,
        request.message.asInstanceOf[FlipcastPushRequest].toJson.compactPrint)
    case _ =>
      ServiceUnhandledResponse()
  }

}
