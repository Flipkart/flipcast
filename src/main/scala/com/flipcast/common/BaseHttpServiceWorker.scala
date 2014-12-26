package com.flipcast.common

import akka.actor.{ActorSystem, Actor}
import akka.event.slf4j.Logger
import akka.util.Timeout
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import com.flipcast.protocol.ServiceProtocolSupport
import com.flipcast.{ServiceState, Flipcast}
import spray.http._
import spray.json._
import com.flipcast.model.responses._
import spray.http.HttpResponse
import com.flipcast.model.requests.InRotationServiceRequest
import com.flipcast.model.requests.OutOfRotationServiceRequest
import com.flipcast.model.responses.InRotationResponse
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.model.requests.ServiceRequest
import spray.routing.RequestContext
import com.flipcast.model.responses.PingServiceResponse
import com.flipcast.push.model.responses.{DeviceDetailsUnregisterSuccessResponse, DeviceDetailsRegisterResponse}

/**
 * Base service actor for handling any service requests
 * Provides enough abstractions for:
 *  1) Handling requests transparently
 *  2) Sending responses
 *  3) Handle unhandled requests
 *
 *  @author Phaneesh Nagaraja
 */
trait BaseHttpServiceWorker extends ServiceProtocolSupport {

  /**
   * Logger instance for service worker
   */
  val log = Logger(this.getClass.getSimpleName)

  /**
   * Default timeout used for ask calls
   */
  implicit val timeout: Timeout = Duration(60, SECONDS)

  /**
   * Actor system that might be required for creating sub-components
   */
  implicit val system: ActorSystem = Flipcast.system

  /**
   * Host name that might become handy
   */
  lazy val host = Flipcast.hostname

  /**
   * Receive any service request that is sent to the service
   * @return Unit
   */
  def execute(message: Any) = {
    message match {
      case request: ServiceRequest[_] =>
        Flipcast.serviceState match {
          case ServiceState.IN_ROTATION => processRequest(request)
          case _ => unavailable(request.ctx)
        }
      case request: InRotationServiceRequest =>
        processRequest(request)
      case request: OutOfRotationServiceRequest =>
        processRequest(request)
    }
  }

  /**
   * Handles all the service requests and sends appropriate HTTP responses
   * Exceptions: InRotation and OutOfRotation will be handled separately
   * @param request ServiceRequest which wraps any type of request
   */
  def processRequest(request: ServiceRequest[_]) {
    try {
      val result = process(request.data)
      result match {
        case r: ServiceSuccessResponse[_] =>
          r.data match {
            case d: String => send[String](StatusCodes.OK, request.ctx, d)
            case d: PingServiceResponse => send[PingServiceResponse](StatusCodes.OK, request.ctx, d)
            case d: StatusCheckResponse => send[StatusCheckResponse](StatusCodes.OK, request.ctx, d)
            case d: UpdatePushConfigResponse => send[UpdatePushConfigResponse](StatusCodes.OK, request.ctx, d)
            case d: DeviceDetailsRegisterResponse => send[DeviceDetailsRegisterResponse](StatusCodes.OK, request.ctx, d)
            case d: DeviceDetailsUnregisterSuccessResponse => send[DeviceDetailsUnregisterSuccessResponse](StatusCodes.OK, request.ctx, d)
            case d: UnicastSuccessResponse => send[UnicastSuccessResponse](StatusCodes.OK, request.ctx, d)
            case d: MulticastSuccessResponse => send[MulticastSuccessResponse](StatusCodes.OK, request.ctx, d)
            case d: GetAllPushHistoryResponse => send[GetAllPushHistoryResponse](StatusCodes.OK, request.ctx, d)
          }
        case r: ServiceUnhandledResponse => unhandled(request.ctx)
        case r: ServiceFailureResponse => send[Throwable](StatusCodes.InternalServerError, request.ctx, r.error)
        case r: ServiceBadRequestResponse => send[String](StatusCodes.BadRequest, request.ctx, r.message)
        case _ => unhandled(request.ctx)
      }
    } catch {
      case ex: Exception =>
        log.error("Error processing request: " +request, ex)
        send[Throwable](StatusCodes.InternalServerError, request.ctx, ex)
    }
  }

  /**
   * Handle request to bring service back into service
   * @param request InRotationServiceRequest
   */
  def processRequest(request: InRotationServiceRequest) {
    val result = process(request)
    result match {
      case r: InRotationResponse =>
        r.result match {
          case true => send[InRotationResponse](StatusCodes.OK, request.ctx, r)
          case false => send[String](StatusCodes.Conflict, request.ctx, "Unable to bring service back to rotation")
        }
      case _ =>
        unhandled(request.ctx)
    }
  }

  /**
   * Handle request to take the service offline
   * @param request OutOfRotationServiceRequest
   */
  def processRequest(request: OutOfRotationServiceRequest) {
    val result = process(request)
    result match {
      case r: OutOfRotationResponse =>
        r.result match {
          case true => send[OutOfRotationResponse](StatusCodes.OK, request.ctx, r)
          case false => send[String](StatusCodes.Conflict, request.ctx, "Unable to bring service back to rotation")
        }
      case _ =>
        unhandled(request.ctx)
    }
  }


  /**
   * Abstract process method which will process the request
   * @param input Request that was wrapped in service request
   * @return One of the subtypes of service response
   */
  def process[T](input: T) : ServiceResponse[_]

  /**
   * Sends back HTTP responses by marshaling different types
   * @param status HTTP status code
   * @param ctx Request context
   * @param response response payload
   * @tparam A Response type
   */
  def send[A](status: StatusCode, ctx: RequestContext, response: A) {
    val httpResponse = response match {
      case r: String =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, r))
      case r : PingServiceResponse =>
          HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: InRotationResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: OutOfRotationResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: StatusCheckResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: UpdatePushConfigResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: DeviceDetailsRegisterResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: DeviceDetailsUnregisterSuccessResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Success"))
      case r: UnicastSuccessResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: MulticastSuccessResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case r: GetAllPushHistoryResponse =>
        HttpResponse(status = status, entity = HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
      case e: Throwable =>
          HttpResponse(status = status, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Oops! Something went wrong!"))
    }
    AccessLogger.access(ctx, httpResponse)
    ctx.complete(httpResponse)
  }

  /**
   * Handles unknown calls and results (Ends up as bad api call)
   * @param ctx Request context required for sending HTTP response
   */
  def unhandled (ctx: RequestContext) {
    ctx.complete(
      HttpResponse(status = StatusCodes.BadRequest, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Bad Request")))
  }

  /**
   * Sends service unavailable status when the service has been taken offline
   * @param ctx Request context required for sending HTTP response
   */
  def unavailable (ctx: RequestContext) {
    ctx.complete(
      HttpResponse(status = StatusCodes.ServiceUnavailable, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Service Unavailable")))
  }
}
