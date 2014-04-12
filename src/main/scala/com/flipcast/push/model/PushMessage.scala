package com.flipcast.push.model

/**
 * Model that represents a push message
 *
 * @author Phaneesh Nagaraja
 */
case class PushMessage(message: String, ttl: Option[Int], delayWhileIdle: Option[Boolean])
