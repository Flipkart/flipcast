package com.flipcast.push.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.push.config._
import com.flipcast.push.config.GcmConfig
import com.flipcast.push.config.ApnsConfig

/**
 * Marshaller and Unmarshaller for push configuration
 *
 * @author Phaneesh Nagaraja
 */
trait PushConfigurationProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val ApnsConfigFormat = jsonFormat4(ApnsConfig)

  implicit val GcmConfigFormat = jsonFormat3(GcmConfig)

  implicit val MpnsConfigFormat = jsonFormat3(MpnsConfig)

  implicit object PushConfigFormat extends RootJsonFormat[PushConfig] {

    def write(obj: PushConfig) = {
      JsObject(
        "configName" -> JsString(obj.configName),
        "apns" -> obj.apns.toJson,
        "gcm" -> obj.gcm.toJson,
        "mpns" -> obj.mpns.toJson
      )
    }

    def read(json: JsValue) = {
      val configName = json.asJsObject.fields.contains("configName") match {
        case true =>
          json.asJsObject.fields("configName") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val apnsConfig = json.asJsObject.fields("apns").convertTo[ApnsConfig]
      val gcmConfig = json.asJsObject.fields("gcm").convertTo[GcmConfig]
      val mpnsConfig = json.asJsObject.fields("mpns").convertTo[MpnsConfig]
      PushConfig(configName, gcmConfig, apnsConfig, mpnsConfig)
    }
  }

}
