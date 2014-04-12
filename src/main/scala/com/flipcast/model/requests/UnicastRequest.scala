package com.flipcast.model.requests

import com.flipcast.push.model.PushMessage

/**
 * Model class for unicast request
 */
case class UnicastRequest(configName: String, filter: Map[String, Any], message: PushMessage)
