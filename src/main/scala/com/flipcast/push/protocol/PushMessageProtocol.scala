package com.flipcast.push.protocol

import com.flipcast.push.model.PushMessage
import spray.httpx.SprayJsonSupport
import spray.json._

/**
 * Serializer and deserializer for push message payload
 *
 * @author Phaneesh Nagaraja
 */
trait PushMessageProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object PushMessageFormat extends RootJsonFormat[PushMessage] {

    def write(obj: PushMessage) = {
      val ttl = obj.ttl match {
        case  Some(x) => JsNumber(x)
        case _ => JsNull
      }
      val delayWhileIdle = obj.delayWhileIdle match {
        case Some(x) => JsBoolean(x)
        case _ => JsNull
      }
      val priority = obj.priority match {
        case Some(x) => JsString(x)
        case _ => JsNull
      }
      JsObject(
        "message" -> JsonParser(obj.message).asJsObject,
        "ttl" -> ttl,
        "delayWhileIdle" -> delayWhileIdle,
        "priority" -> priority
      )
    }

    def read(json: JsValue) = {
      val ttl = json.asJsObject.fields.contains("ttl") match {
        case true =>
          json.asJsObject.fields("ttl") match {
            case a: JsNumber => Option(a.value.toInt)
            case _ => None
          }
        case _ => None
      }
      val delayWhileIdle = json.asJsObject.fields.contains("delayWhileIdle") match {
        case true =>
          json.asJsObject.fields("delayWhileIdle") match {
            case a: JsBoolean => Option(a.value)
            case _ => None
          }
        case _ => None
      }
      val priority = json.asJsObject.fields.contains("priority") match {
        case true =>
          json.asJsObject.fields("priority") match {
            case a: JsString => Option(a.value)
            case _ => None
          }
        case _ => None
      }
      val message = json.asJsObject.fields.contains("message") match {
        case true => json.asJsObject.fields("message").compactPrint
        case _ => "{}"
      }
      PushMessage(message, ttl, delayWhileIdle, priority)
    }
  }
}
