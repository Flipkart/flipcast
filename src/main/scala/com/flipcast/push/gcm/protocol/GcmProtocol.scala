package com.flipcast.push.gcm.protocol

import spray.json._
import spray.httpx.SprayJsonSupport
import com.flipcast.push.gcm.model.{GcmResponse, GcmRequest, GcmResult}

/**
 * GCM Protocol JSON marshaller and unmarshaller
 *
 * @author Phaneesh Nagaraja
 */
trait GcmProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object GcmResultFormat extends RootJsonFormat[GcmResult] {

    def write(obj: GcmResult) = {
      val messageId = obj.message_id match {
        case None => JsNull
        case Some(x) => JsString(x)
      }
      val regId = obj.registration_id match {
        case None => JsNull
        case Some(x) => JsString(x)
      }
      val error = obj.error match {
        case None => JsNull
        case Some(x) => JsString(x)
      }
      JsObject("message_id" -> messageId,
        "registration_id" -> regId,
        "error" -> error)
    }

    def read(obj: JsValue) = {
      val message_id = obj.asJsObject.fields.contains("message_id") match {
        case true =>
          obj.asJsObject.fields("message_id") match {
            case JsString(id) => Some(id)
            case _ => None
          }
        case false => None
      }
      val registration_id = obj.asJsObject.fields.contains("registration_id") match {
        case true =>
          obj.asJsObject.fields("registration_id") match {
            case JsString(id) => Some(id)
            case _ => None
          }
        case false => None
      }
      val error = obj.asJsObject.fields.contains("error") match {
        case true =>
          obj.asJsObject.fields("error") match {
            case JsString(er) => Some(er)
            case _ => None
          }
        case false => None
      }
      GcmResult(message_id, registration_id, error)
    }
  }

  implicit object GcmRequestFormat extends RootJsonFormat[GcmRequest] {

    def write(obj: GcmRequest) = {
      JsObject(
        "registration_ids" -> JsArray(obj.registration_id.map(JsString(_))),
        "delay_while_idle" -> JsBoolean(obj.delay_while_idle),
        "time_to_live" -> JsNumber(obj.time_to_live),
        "data" -> JsonParser(obj.data)
      )
    }

    def read(obj: JsValue) = {
      val registration_ids = obj.asJsObject.fields.contains("registration_ids") match {
        case true =>
          obj.asJsObject.fields("registration_ids") match {
            case JsArray(s) => s.map {
              case JsString(r) => r
              case _ => "None"
            }.filter( _ != "None").toList
            case _ => List.empty
          }
        case false => List.empty
      }
      val data = obj.asJsObject.fields.contains("data") match {
        case true => obj.asJsObject.fields("data").compactPrint
        case false => "{}"
      }
      val delay_while_idle = obj.asJsObject.fields.contains("delay_while_idle") match {
        case true =>
          obj.asJsObject.fields("delay_while_idle") match {
            case JsBoolean(s) => s
            case _ => false
          }
        case false => false
      }
      val time_to_live = obj.asJsObject.fields.contains("time_to_live") match {
        case true =>
          obj.asJsObject.fields("time_to_live") match {
            case JsNumber(s) => s.intValue()
            case _ => 0
          }
        case false => 0
      }
      GcmRequest(registration_ids, data, delay_while_idle, time_to_live)
    }
  }

  implicit object GcmResponseFormat extends RootJsonFormat[GcmResponse] {

    def write(obj: GcmResponse) = {
      val multicastId = obj.multicast_id match {
        case None => JsNull
        case Some(x) => JsString(x)
      }
      JsObject(
        "multicast_id" -> multicastId,
        "success" -> JsNumber(obj.success),
        "failure" -> JsNumber(obj.failure),
        "canonical_ids" -> JsNumber(obj.canonical_ids),
        "results" -> JsArray(obj.results.map(GcmResultFormat.write))
      )
    }

    def read(obj: JsValue) = {
      val multicast_id = obj.asJsObject.fields.contains("multicast_id") match {
        case true =>
          obj.asJsObject.fields("multicast_id") match {
            case JsString(id) => Some(id)
            case _ => None
          }
        case false => None
      }
      val success = obj.asJsObject.fields.contains("success") match {
        case true =>
          obj.asJsObject.fields("success") match {
            case JsNumber(s) => s.toInt
            case _ => 0
          }
        case false => 0
      }
      val failure = obj.asJsObject.fields.contains("failure") match {
        case true =>
          obj.asJsObject.fields("failure") match {
            case JsNumber(s) => s.toInt
            case _ => 0
          }
        case false => 0
      }
      val canonical_ids = obj.asJsObject.fields.contains("canonical_ids") match {
        case true =>
          obj.asJsObject.fields("canonical_ids") match {
            case JsNumber(s) => s.toInt
            case _ => 0
          }
        case false => 0
      }
      val results = obj.asJsObject.fields.contains("results") match {
        case true =>
          obj.asJsObject.fields("results") match {
            case JsArray(r) => r.map(GcmResultFormat.read).toList
            case _ => List()
          }
        case false => List()
      }
      GcmResponse(multicast_id, success, failure, canonical_ids, results)
    }
  }

}
