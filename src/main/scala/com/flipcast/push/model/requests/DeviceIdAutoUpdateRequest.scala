package com.flipcast.push.model.requests

/**
 * Model class for device id auto update
 *
 * @author Phaneesh Nagaraja
 */
case class DeviceIdAutoUpdateRequest(configName: String, oldDeviceIdentifier: String, newDeviceIdentifier: String)
