package com.flipcast.push.apns.protocol

import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport
import com.flipcast.push.apns.model.ApnsRequest

/**
 * Marshaller & UnMarshaller for APNS request
 */
trait ApnsProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  val ApnsRequestFormat = jsonFormat3(ApnsRequest)

}
