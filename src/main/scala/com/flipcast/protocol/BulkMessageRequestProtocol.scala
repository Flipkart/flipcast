package com.flipcast.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.model.requests.BulkMessageRequest
import com.flipcast.push.protocol.PushMessageProtocol
import com.flipcast.push.model.PushMessage

/**
 * Serializer and deserializer for bulk message requests
 *
 * @author Phaneesh Nagaraja
 */
trait BulkMessageRequestProtocol extends DefaultJsonProtocol with SprayJsonSupport with PushMessageProtocol {

  implicit object BulkMessageRequestFormat extends RootJsonFormat[BulkMessageRequest] {

    def write(obj: BulkMessageRequest) = {
      val query = obj.query.map( q => {
        val v = q._2 match {
          case v: String => JsString(v)
          case v: Int => JsNumber(v)
          case v: Long => JsNumber(v)
          case v: Double => JsNumber(v)
          case v: Float => JsNumber(v)
          case v: Boolean => JsBoolean(v)
          case _ => JsString(q._2.toString)
        }
        q._1 -> v
      }).toMap
      JsObject(
        "configName" -> JsString(obj.configName),
        "query" -> JsObject(query),
        "message" -> obj.message.toJson,
        "start" -> JsNumber(obj.start),
        "end" -> JsNumber(obj.end)
      )
    }

    def read(json: JsValue) = {
      val configName = json.asJsObject.fields.contains("configName") match {
        case true =>
          json.asJsObject.fields("configName") match {
            case s: JsString => s.value
            case _ => "NA"
          }
        case false => "NA"
      }
      val query = json.asJsObject.fields.contains("query") match {
        case true =>
          json.asJsObject.fields("query").asJsObject.fields.map( f => {
            val v = f._2 match {
              case JsString(v) => v
              case JsNumber(v) => v.toLong
              case JsBoolean(v) => v
              case _ => f._2.compactPrint
            }
            f._1 -> v
          }).toMap
        case false => Map.empty[String, Any]
      }
      val start = json.asJsObject.fields.contains("start") match {
        case true =>
          json.asJsObject.fields("start") match {
            case a: JsNumber => a.value.toInt
            case _ => 0
          }
        case _ => 0
      }
      val end = json.asJsObject.fields.contains("end") match {
        case true =>
          json.asJsObject.fields("end") match {
            case a: JsNumber => a.value.toInt
            case _ => 0
          }
        case _ => 0
      }
      val message = json.asJsObject.fields.contains("message") match {
        case true => json.asJsObject.fields("message").convertTo[PushMessage]
        case _ => PushMessage("{}", Option(0), Option(true))
      }
      BulkMessageRequest(configName, query, message, start, end)
    }
  }
}
