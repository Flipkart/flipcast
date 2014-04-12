package com.flipcast.model.requests

import com.flipcast.push.model.PushMessage

/**
 * Model to represent broadcast request
 *
 * @author Phaneesh Nagaraja
 */
case class BroadcastRequest(configName: String, message: PushMessage)
