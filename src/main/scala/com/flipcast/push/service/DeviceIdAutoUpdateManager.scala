package com.flipcast.push.service

import akka.actor.Actor
import com.flipcast.push.common.DeviceDataSourceManager
import akka.event.slf4j.Logger
import com.flipcast.push.model.requests.DeviceIdAutoUpdateRequest

/**
 * Automatically update
 */
class DeviceIdAutoUpdateManager extends Actor {

  val log = Logger("DeviceIdAutoUpdateManager")

  def receive = {
    case request: DeviceIdAutoUpdateRequest =>
      try {
        DeviceDataSourceManager.dataSource(request.configName).autoUpdateDeviceId(request.configName, request.oldDeviceIdentifier,
          request.newDeviceIdentifier)
      } catch {
        case ex: Exception =>
          log.error("Error updating device id:", ex)
      }
    case _ =>
      log.warn("Unknown message received")
  }

}
