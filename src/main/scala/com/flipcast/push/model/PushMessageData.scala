package com.flipcast.push.model

import scala.collection.mutable

/**
 * Model to represent push message payload for different platforms
 *
 * @author Phaneesh Nagaraja
 */
class PushMessageData() {

  private val data = new mutable.HashMap[DeviceOperatingSystemType.Value, String]()

  def addPayload(osType: DeviceOperatingSystemType.Value, payload: String) {
    data.put(osType, payload)
  }

  def getPayload(osType: DeviceOperatingSystemType.Value) : Option[String] = {
    data.contains(osType) match {
      case true => Option(data(osType))
      case false => None
    }
  }

}
