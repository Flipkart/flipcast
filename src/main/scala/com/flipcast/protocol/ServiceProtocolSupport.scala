package com.flipcast.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.model.responses._
import com.flipcast.push.protocol.DeviceDataProtocol
import com.flipcast.push.model.{DeviceOperatingSystemType, DeviceData}
import com.flipcast.push.model.responses.DeviceDetailsRegisterResponse
import com.flipcast.model.responses.StatusCheckResponse
import com.flipcast.model.responses.InRotationResponse
import com.flipcast.model.responses.PingServiceResponse
import com.flipcast.push.model.DeviceData
import com.flipcast.model.responses.OutOfRotationResponse
import com.flipcast.push.model.responses.DeviceDetailsRegisterResponse
import com.flipcast.model.responses.UpdatePushConfigResponse

/**
 * JSON protocol support for all models (request/response) used in the service
 *
 * @author Phaneesh Nagaraja
 */
trait ServiceProtocolSupport extends DefaultJsonProtocol with SprayJsonSupport with DeviceDataProtocol {

  /**
   * JSON format for ping response
   */
  implicit val PingServiceResponseFormat = jsonFormat3(PingServiceResponse)

  /**
   * JSON format for status check
   */
  implicit val StatusCheckResponseFormat = jsonFormat1(StatusCheckResponse)

  /**
   * JSON format for push configuration update
   */
  implicit val UpdatePushConfigResponseFormat = jsonFormat1(UpdatePushConfigResponse)

  /**
   * JSON format for in rotation response
   */
  implicit val InRotationResponseFormat = jsonFormat1(InRotationResponse)

  /**
   * JSON format for out of rotation response
   */
  implicit val OutOfRotationResponseFormat = jsonFormat1(OutOfRotationResponse)

  /**
   * JSON format for unicast success response
   */
  implicit val UnicastSuccessResponseFormat = jsonFormat2(UnicastSuccessResponse)

  /**
   * JSON format for multicast success response
   */
  implicit val MulticastSuccessResponseFormat = jsonFormat2(MulticastSuccessResponse)

  /**
   * JSON format for device details registration request
   */
  implicit object DeviceDetailsRegisterResponseFormat extends RootJsonFormat[DeviceDetailsRegisterResponse] {

    def write(obj: DeviceDetailsRegisterResponse) = {
      JsObject(
        "device" -> obj.deviceData.toJson
      )
    }

    def read(json: JsValue) = {
      val device = json.asJsObject.fields.contains("device") match {
        case true =>
          Option(json.asJsObject.fields("device").convertTo[DeviceData])
        case false => None
      }
      DeviceDetailsRegisterResponse(deviceData = device.getOrElse(DeviceData("Unknown","Unknown","Unknown",
        DeviceOperatingSystemType.Unknown,"Unknown","Unknown","Unknown","Unknown","Unknown")))
    }
  }
}
