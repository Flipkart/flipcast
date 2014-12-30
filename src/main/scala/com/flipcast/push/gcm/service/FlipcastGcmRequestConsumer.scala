package com.flipcast.push.gcm.service

import com.flipcast.push.common.FlipcastRequestConsumer
import com.flipcast.push.protocol.FlipcastPushProtocol
import spray.client.HttpDialog
import spray.httpx.RequestBuilding._
import scala.concurrent.{ExecutionContext, Await}
import com.flipcast.push.gcm.model.{GcmResponse, GcmRequest}
import spray.http.StatusCodes
import com.flipcast.push.gcm.protocol.GcmProtocol
import com.flipcast.Flipcast
import ExecutionContext.Implicits.global
import scala.collection.mutable.ListBuffer
import spray.json._
import com.flipcast.push.model.requests.{RecordPushHistoryRequest, FlipcastPushRequest, DeviceHousekeepingRequest, DeviceIdAutoUpdateRequest}

/**
 * Message Consumer for GCM requests
 * Will use the GCM endpoint to send the messages using spray-httpclient
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastGcmRequestConsumer extends FlipcastRequestConsumer[FlipcastPushRequest] with FlipcastPushProtocol with GcmProtocol {

  override def configType() = "gcm"

  override def init() {

  }

  override def consume(request: FlipcastPushRequest) =  {
    val service = GCMServicePool.service(request.configName)
    val config = Flipcast.pushConfigurationProvider.config(request.configName).gcm
    val delayWhileIdle = request.delayWhileIdle match {
      case Some(x) => x
      case _ => config.defaultDelayWhileIdle
    }
    val ttl = request.ttl match {
      case Some(x) =>
        if(x > 2592000) 2592000 //Default it to 4 weeks if the ttl is greater than 4 weeks
        else x
      case _ =>  config.defaultExpiry
    }
    val gcmRequest = GcmRequest(request.registration_ids, request.data, delayWhileIdle, ttl)
    val gcmResponse = HttpDialog(service.client)
      .send(Post("/gcm/send", gcmRequest).withHeaders(service.headers))
      .end
    val gcmResult = Await.result(gcmResponse, DEFAULT_TIMEOUT)
    val failedIds = new ListBuffer[String]()
    gcmResult.status match {
      case StatusCodes.OK =>
        val gcmResponse = JsonParser(gcmResult.entity.asString).convertTo[GcmResponse]
        gcmResponse.failure == 0 match {
          case true => None
          case false =>
            var idx: Int = 0
            gcmResponse.results.foreach(res => {
              res.error match {
                case Some("MissingRegistration") =>
                  log.warn("Missing registration id: " + request.registration_ids(idx))
                  //Housekeeping
                  Flipcast.serviceRegistry.actor("deviceHouseKeepingManager") ! DeviceHousekeepingRequest(request.configName, request.registration_ids(idx))
                case Some("InvalidRegistration") =>
                  log.warn("Invalid registration: " + request.registration_ids(idx))
                  //Housekeeping
                  Flipcast.serviceRegistry.actor("deviceHouseKeepingManager") ! DeviceHousekeepingRequest(request.configName, request.registration_ids(idx))
                case Some("MismatchSenderId") =>
                  log.warn("Mismatch sender Id: " + request.registration_ids(idx))
                  //Housekeeping
                  Flipcast.serviceRegistry.actor("deviceHouseKeepingManager") ! DeviceHousekeepingRequest(request.configName, request.registration_ids(idx))
                case Some("NotRegistered") =>
                  log.warn("Not registered: " + request.registration_ids(idx))
                  //Housekeeping
                  Flipcast.serviceRegistry.actor("deviceHouseKeepingManager") ! DeviceHousekeepingRequest(request.configName, request.registration_ids(idx))
                case Some("MessageTooBig") =>
                  log.warn("Message too big: " + request.registration_ids(idx))
                case Some("InvalidDataKey") =>
                  log.warn("Invalid data key: " + request.registration_ids(idx))
                case Some("InvalidTtl") =>
                  log.warn("Invalid TTL: " + request.registration_ids(idx))
                case Some("InternalServerError") =>
                  failedIds += request.registration_ids(idx)
                  log.warn("Internal Server Error: " + request.registration_ids(idx))
                case Some("Unavailable") =>
                  failedIds += request.registration_ids(idx)
                  log.warn("Unavailable: " + request.registration_ids(idx))
                case Some("InvalidPackageName") =>
                  log.warn("Invalid Package Name: " + request.registration_ids(idx))
                case _ =>
                  log.warn("Unknown Error: " + request.registration_ids(idx))
                  failedIds += request.registration_ids(idx)
              }
              idx = idx + 1
            })
          }
          var idx: Int = 0
          gcmResponse.canonical_ids == 0 match {
            case true => None
            case false =>
              gcmResponse.results.foreach( reg => {
                reg.registration_id match {
                  case Some(newId) =>
                    Flipcast.serviceRegistry.actor("deviceIdAutoUpdateManager") ! DeviceIdAutoUpdateRequest(request.configName, request.registration_ids(idx), newId)
                  case _ => None
                }
                idx = idx + 1
              })
            }
            failedIds.isEmpty match {
              case true => None
              case false =>
                resend(FlipcastPushRequest(request.configName, failedIds.toList, request.data, request.ttl, request.delayWhileIdle, request.priority))
            }
            //Record history for all successful devices
            request.registration_ids.diff(failedIds.toList).par.foreach( r => {
              Flipcast.serviceRegistry.actor("pushMessageHistoryManager") ! RecordPushHistoryRequest[FlipcastPushRequest](request.configName, r, request)
            })
      case StatusCodes.Unauthorized =>
        log.warn("Unauthorized: " + request)
      case StatusCodes.BadRequest =>
        log.warn("Bad Request: " + request)
      case StatusCodes.InternalServerError =>
        log.warn("Internal Server Error: " + request)
        resend(request)
      case StatusCodes.ServiceUnavailable =>
        log.warn("Service Unavailable: " + request)
        resend(request)
      case StatusCodes.GatewayTimeout =>
        log.warn("Gateway Timeout: " + request)
        resend(request)
      case _ =>
        resend(request)
      }
    true
  }
}
