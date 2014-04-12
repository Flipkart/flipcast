package com.flipcast.push.model.requests

import spray.json.JsObject

/**
 * Request model for register device details
 *
 * @author Phaneesh Nagaraja
 */
case class DeviceDetailsRegisterRequest(configName: String, deviceData: JsObject, filter: Map[String, Any])
