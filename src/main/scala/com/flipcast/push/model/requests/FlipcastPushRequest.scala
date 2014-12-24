package com.flipcast.push.model.requests


@SerialVersionUID(1L)
trait FlipcastRequest

/**
 * Model class for push message request
 * @param configName Name of the configuration
 * @param registration_ids Array of device registration ids
 * @param data payload that needs to be sent as message
 */
case class FlipcastPushRequest(configName: String,
                               registration_ids: List[String],
                               data: String,
                               ttl: Option[Int],
                               delayWhileIdle: Option[Boolean]) extends FlipcastRequest
