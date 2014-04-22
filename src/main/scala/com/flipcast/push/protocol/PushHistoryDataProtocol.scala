package com.flipcast.push.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.push.model.PushHistoryData

/**
 * Serializer and deserializer for PushHistoryData
 */
trait PushHistoryDataProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object PushHistoryDataFormat extends RootJsonFormat[PushHistoryData] {

    def write(obj: PushHistoryData) = {
      JsObject(
        "total" -> JsNumber(obj.total),
        "data" -> JsObject(obj.data.map( e => {e._1 -> JsNumber(e._2)}))
      )
    }

    def read(json: JsValue) = {
      val total = json.asJsObject.fields.contains("total") match {
        case true =>
          json.asJsObject.fields("total") match {
            case s: JsNumber => s.value.toLong
            case _ => 0L
          }
        case false => 0L
      }
      val data = json.asJsObject.fields.contains("data") match {
        case true =>
          json.asJsObject.fields("data").asJsObject.fields.map(e => {
            val v = e._2 match {
              case x: JsNumber => x.value.toLong
              case _ => 0L
            }
            e._1 -> v
          }).toMap
        case false => Map.empty[String, Long]
      }
      PushHistoryData(total, data)
    }
  }
}
