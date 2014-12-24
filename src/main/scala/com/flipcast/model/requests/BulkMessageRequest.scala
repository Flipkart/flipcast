package com.flipcast.model.requests

import com.flipcast.push.model.PushMessage
import com.flipcast.push.model.requests.FlipcastRequest

/**
 * Model to represent a bulk message request
 *
 * @author Phaneesh Nagaraja
 */
case class BulkMessageRequest(configName: String, query: Map[String, Any],
                              message: PushMessage,
                              start: Int,
                              end: Int) extends FlipcastRequest
