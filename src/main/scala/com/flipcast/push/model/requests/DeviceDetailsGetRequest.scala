package com.flipcast.push.model.requests

/**
 * Model class to represent get device details request
 */
case class DeviceDetailsGetRequest(configName: String, filter: Map[String, Any])
