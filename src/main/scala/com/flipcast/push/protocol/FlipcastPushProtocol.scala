package com.flipcast.push.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.push.model.requests.FlipcastPushRequest

/**
 * Protocol support for marshalling and unmarshalling FlipcastGcmRequest
 *
 * @author Phaneesh Nagaraja
 */
trait FlipcastPushProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object FlipcastPushRequestFormat extends RootJsonFormat[FlipcastPushRequest] {

    def write(obj: FlipcastPushRequest) = {
      val ttl = obj.ttl match {
        case  Some(x) => JsNumber(x)
        case _ => JsNumber(0)
      }
      val delayWhileIdle = obj.delayWhileIdle match {
        case Some(x) => JsBoolean(x)
        case _ => JsBoolean(x = false)
      }
      val data = try {
        JsonParser(obj.data).asJsObject
      } catch {
        case ex: Exception => JsString(obj.data)
      }
      JsObject(
        "configName" -> JsString(obj.configName),
        "registration_ids" -> JsArray(obj.registration_ids.filter( _.trim.length > 0).map( JsString(_))),
        "data" -> data,
        "ttl" -> ttl,
        "delayWhileIdle" -> delayWhileIdle
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
      val registration_ids = json.asJsObject.fields("registration_ids") match {
        case x: JsArray => x.elements.map {
          case a: JsString => a.value
          case _ => ""
        }.toList
        case _ => List()
      }
      val ttl = json.asJsObject.fields.contains("ttl") match {
        case true =>
          json.asJsObject.fields("ttl") match {
            case a: JsNumber => a.value.toInt
            case _ => 0
          }
        case _ => 0
      }
      val delayWhileIdle = json.asJsObject.fields.contains("delayWhileIdle") match {
        case true =>
          json.asJsObject.fields("delayWhileIdle") match {
            case a: JsBoolean => a.value
            case _ => false
          }
        case _ => false
      }
      val data = json.asJsObject.fields.contains("data") match {
        case true => json.asJsObject.fields("data").compactPrint
        case _ => "{}"
      }
      FlipcastPushRequest(configName, registration_ids.filter( _.trim.length > 0), data, Option(ttl),
        Option(delayWhileIdle))
    }
  }

}
