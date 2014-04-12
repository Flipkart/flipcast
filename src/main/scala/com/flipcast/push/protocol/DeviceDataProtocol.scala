package com.flipcast.push.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.push.model.{DeviceOperatingSystemType, DeviceData}

/**
 * Marshaller & Unmarshaller for Device data
 *
 * @author Phaneesh Nagaraja
 */
trait DeviceDataProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object DeviceDataFormat extends RootJsonFormat[DeviceData] {

    def write(obj: DeviceData) = {
      JsObject(
        "configName" -> JsString(obj.configName),
        "deviceId" -> JsString(obj.deviceId),
        "cloudMessagingId" -> JsString(obj.cloudMessagingId),
        "osName" -> JsString(obj.osName.toString),
        "osVersion" -> JsString(obj.osVersion),
        "brand" -> JsString(obj.brand),
        "model" -> JsString(obj.model),
        "appName" -> JsString(obj.appName),
        "appVersion" -> JsString(obj.appVersion)
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
      val deviceId = json.asJsObject.fields.contains("deviceId") match {
        case true =>
          json.asJsObject.fields("deviceId") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val cloudMessagingId = json.asJsObject.fields.contains("cloudMessagingId") match {
        case true =>
          json.asJsObject.fields("cloudMessagingId") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val osName = json.asJsObject.fields.contains("osName") match {
        case true =>
          json.asJsObject.fields("osName") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val osVersion = json.asJsObject.fields.contains("osVersion") match {
        case true =>
          json.asJsObject.fields("osVersion") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val brand = json.asJsObject.fields.contains("brand") match {
        case true =>
          json.asJsObject.fields("brand") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val model = json.asJsObject.fields.contains("model") match {
        case true =>
          json.asJsObject.fields("model") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val appName = json.asJsObject.fields.contains("appName") match {
        case true =>
          json.asJsObject.fields("appName") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      val appVersion = json.asJsObject.fields.contains("appVersion") match {
        case true =>
          json.asJsObject.fields("appVersion") match {
            case s: JsString => s.value
            case _ => "Unknown"
          }
        case false => "Unknown"
      }
      DeviceData(configName, deviceId, cloudMessagingId, DeviceOperatingSystemType.withName(osName), osVersion, brand,
        model,appName, appVersion)
    }
  }

}
