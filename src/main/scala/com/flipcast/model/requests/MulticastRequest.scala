package com.flipcast.model.requests

import com.flipcast.push.model.PushMessage

/**
 * Model that represents a multicast request
 *
 * @author Phaneesh Nagaraja
 */
case class MulticastRequest (configName: String, filter: Map[String, Any], message: PushMessage)
