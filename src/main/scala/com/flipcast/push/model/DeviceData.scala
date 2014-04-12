package com.flipcast.push.model

/**
 * Model representing device data
 *
 * @author Phaneesh Nagaraja
 */
case class DeviceData(configName: String, deviceId: String,
                      cloudMessagingId: String,
                      osName: DeviceOperatingSystemType.Value,
                      osVersion: String,
                      brand: String,
                      model: String,
                      appName: String,
                      appVersion: String)


object DeviceOperatingSystemType extends Enumeration {

//  type DeviceOperatingSystemType = Value

  val ANDROID = Value("ANDROID")

  val iOS = Value("iOS")

  val WindowsPhone = Value("WINDOWS_PHONE")

  val Unknown = Value("Unknown")

}