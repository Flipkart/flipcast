package com.flipcast.model.responses

import com.flipcast.Flipcast
import spray.http.DateTime

/**
 * Model class for Ping service response
 * @param host Name of the host that is responding to ping request
 * @param time Time in RFC 1123 format on host
 * @param message A obligatory "Pong" message
 *
 * @author Phaneesh Nagaraja
 */
case class PingServiceResponse (host: String = Flipcast.hostname,
                                time: String  = DateTime.now.toRfc1123DateTimeString,
                                message: String = "Pong") extends ServiceResponse
