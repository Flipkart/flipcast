package com.flipcast.push.example

import spray.json._
import spray.httpx.SprayJsonSupport

/**
 * Model for represent default push message
 * @param title Title of the message
 * @param message Message content
 */
case class DefaultPushMessage(title: String, message: String)

/**
 * Model for represent GCM push message
 * @param title Title of the message
 * @param message Message content
 */
case class DefaultGcmMessage(title: String, message: String)

/**
 * Model for represent apns push message
 * @param message Message content
 */
case class DefaultApnsMessage(message: String)

/**
 * Model for represent mpns toast push message
 * @param message Message content
 */
case class DefaultMpnsMessage(message: String)



/**
 * JSON protocol support Default push message
 *
 * @author Phaneesh Nagaraja
 */
trait DefaultPushMessageProtocolSupport extends DefaultJsonProtocol with SprayJsonSupport {

  /**
   * JSON format for default push message
   */
  implicit val DefaultPushMessageFormat = jsonFormat2(DefaultPushMessage)

  /**
   * JSON format for default gcm message
   */
  implicit object DefaultGcmMessageFormat extends RootJsonFormat[DefaultGcmMessage] {

    def write(obj: DefaultGcmMessage) = {
      JsObject(
        "contentTitle" -> JsString(obj.title),
        "contentText" -> JsString(obj.message),
        "tickerText" -> JsString(obj.message)
      )
    }

    def read(json: JsValue) = {
      json.asJsObject.getFields("contentTitle", "contentText") match {
        case Seq(JsString(contentTitle), JsString(contentText), JsString(tickerText)) =>
          DefaultGcmMessage(contentTitle, contentText)
        case _ =>
          DefaultGcmMessage("Unknown", "Unknown")
      }
    }
  }

  /**
   * JSON format for default apns message
   */
  implicit object DefaultApnsMessageFormat extends RootJsonFormat[DefaultApnsMessage] {

    def write(obj: DefaultApnsMessage) = {
      JsObject(
        "aps" -> JsObject(
          "alert" -> JsString(obj.message),
          "badge" -> JsNumber(0),
          "sound" -> JsString("default")
        )
      )
    }

    def read(json: JsValue) = {
      DefaultApnsMessage("Unknown")
    }
  }

}