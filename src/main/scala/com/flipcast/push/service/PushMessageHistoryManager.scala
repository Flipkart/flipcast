package com.flipcast.push.service

import akka.actor.Actor
import akka.event.slf4j.Logger
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.push.model.requests.RecordPushHistoryRequest
import com.flipcast.push.common.DeviceDataSourceManager

/**
 * Keep track of push history
 */
class PushMessageHistoryManager extends Actor {

  val log = Logger("DeviceHouseKeepingManager")

  def receive = {
    case request: RecordPushHistoryRequest =>
      DeviceDataSourceManager.dataSource(request.configName).recordHistory(request.configName, request.key, request.message)
    case _ =>
      ServiceUnhandledResponse()
  }

}
