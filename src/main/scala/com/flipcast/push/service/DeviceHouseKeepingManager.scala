package com.flipcast.push.service

import akka.actor.Actor
import com.flipcast.push.common.DeviceDataSourceManager
import akka.event.slf4j.Logger
import com.flipcast.push.model.requests.DeviceHousekeepingRequest

/**
 * Automatic housekeeping manager to clean up invalid devices from device data source
 *
 * @author Phaneesh Nagaraja
 */
class DeviceHouseKeepingManager extends Actor {

  val log = Logger("DeviceHouseKeepingManager")

  def receive = {
    case request: DeviceHousekeepingRequest =>
      try {
        DeviceDataSourceManager.dataSource(request.configName).doHouseKeeping(request.configName, request.deviceIdentifier)
      } catch {
        case ex: Exception =>
          log.error("Error housekeeping:", ex)
      }
    case _ =>
      log.warn("Unknown message received")
  }
}
