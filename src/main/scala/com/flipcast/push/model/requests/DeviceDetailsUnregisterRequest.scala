package com.flipcast.push.model.requests

/**
 * Request model for deleting device details
 *
 * @author Phaneesh Nagaraja
 */
case class DeviceDetailsUnregisterRequest(configName: String, filter: Map[String, Any])
