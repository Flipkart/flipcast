package com.flipcast.push.mpns.service

import com.flipcast.push.common.FlipcastRequestConsumer
import com.flipcast.push.protocol.FlipcastPushProtocol
import com.flipcast.push.gcm.protocol.GcmProtocol
import spray.can.Http
import spray.http.{StatusCodes, HttpHeaders}
import java.util.UUID
import spray.client.HttpDialog
import spray.httpx.RequestBuilding._
import com.flipcast.push.model.requests.{DeviceHousekeepingRequest, RecordPushHistoryRequest, FlipcastPushRequest}
import java.net.URL
import scala.concurrent.{ExecutionContext, Await}
import ExecutionContext.Implicits.global
import com.flipcast.Flipcast
import scala.collection.mutable.ListBuffer
import spray.json._
import org.apache.commons.codec.binary.Base64
import scala.xml.XML

/**
 * Message Consumer for MPNS requests
 * Will use the MPNS endpoint to send the messages using spray-httpclient
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastMpnsRequestConsumer extends FlipcastRequestConsumer with FlipcastPushProtocol with GcmProtocol {

  override def configType() = "mpns"

  override def init() {

  }

  override def consume(message: String) = {
    val request = JsonParser(message).convertTo[FlipcastPushRequest]
//    val config = Flipcast.pushConfigurationProvider.config(request.configName).mpns
    val failedIds = new ListBuffer[String]()
    val invalidDevices = new ListBuffer[String]()
    request.registration_ids.foreach( r => {
      val svcUrl = new URL(r)
      val isSSL = svcUrl.getProtocol match {
        case "https" => true
        case _ => false
      }
      val mpnsHttpClient = Http.Connect(svcUrl.getHost, sslEncryption = isSSL)
      val mId = HttpHeaders.RawHeader("X-MessageID", UUID.randomUUID().toString)
      val mType = HttpHeaders.RawHeader("X-NotificationClass", "2")
      val mTarget = HttpHeaders.RawHeader("X-WindowsPhone-Target", "toast")
      val lstHeaders = List(mId, mType, mTarget)
      val decodedMessage = new String(Base64.decodeBase64(request.data.getBytes))
      val payload = XML.loadString(decodedMessage)
      val mpnsResponse = HttpDialog(mpnsHttpClient)
        .send(Post(svcUrl.getPath, payload)
        .withHeaders(lstHeaders))
        .end
      val mpnsResult = Await.result(mpnsResponse, DEFAULT_TIMEOUT)
      mpnsResult.status match {
        case StatusCodes.OK =>
          Flipcast.serviceRegistry.actor("pushMessageHistoryManager") ! RecordPushHistoryRequest(request.configName, r, message)
          true
        case StatusCodes.BadRequest =>
          log.warn("[MPNS] Bad Request: " +mpnsResult.status )
          invalidDevices += r
        case StatusCodes.Unauthorized =>
          log.warn("[MPNS] Unauthorized Request: " +mpnsResult.status )
          invalidDevices += r
        case StatusCodes.NotFound =>
          log.warn("[MPNS] Could not deliver message: " +mpnsResult.status )
          invalidDevices += r
        case StatusCodes.NotAcceptable =>
          log.warn("[MPNS] Could not deliver message: " +mpnsResult.status )
          failedIds += r
        case StatusCodes.PreconditionFailed =>
          log.warn("[MPNS] Could not deliver message: " +mpnsResult.status )
          invalidDevices += r
          false
        case StatusCodes.ServiceUnavailable =>
          log.warn("[MPNS] Service Unavailable. Retrying: " +mpnsResult.status )
          failedIds += r
        case _ =>
          log.warn("[MPNS] Service Unavailable. Retrying: " +mpnsResult.status )
          failedIds += r
      }
    })
    invalidDevices.foreach( r => {
      Flipcast.serviceRegistry.actor("deviceHouseKeepingManager") ! DeviceHousekeepingRequest(request.configName, r)
    })
    failedIds.size > 0 match {
      case true =>
        val retryRequest = FlipcastPushRequest(request.configName, failedIds.toList, request.data, request.ttl, request.delayWhileIdle).toJson.compactPrint
        resend(retryRequest)
      case false =>
        None
    }
    true
  }

}
