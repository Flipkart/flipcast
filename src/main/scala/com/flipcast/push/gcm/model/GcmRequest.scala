package com.flipcast.push.gcm.model

/**
 * Model class to represent GCM request
 * @param registration_id registration id of the device
 * @param data payload that needs to be sent as message
 */
case class GcmRequest(registration_id: List[String], data: String, delay_while_idle: Boolean, time_to_live: Int)
