package com.flipcast.model.requests

import com.flipcast.push.model.PushMessage

/**
 * Model to represent a bulk message request
 *
 * @author Phaneesh Nagaraja
 */
case class BulkMessageRequest(configName: String, query: Map[String, Any],
                              message: PushMessage,
                              start: Int,
                              end: Int)
