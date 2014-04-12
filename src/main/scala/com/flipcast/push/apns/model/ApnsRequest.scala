package com.flipcast.push.apns.model

/**
 * Model to represent apns request
 *
 * @author Phaneesh Nagaraja
 */
case class ApnsRequest(token: String, payload: String, expiry: Int)
