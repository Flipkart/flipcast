package com.flipcast.services

import com.flipcast.common.{BaseHttpService, BaseHttpServiceWorker}
import com.flipcast.model.requests.ServiceRequest
import com.flipcast.model.responses.{ServiceBadRequestResponse, ServiceNotFoundResponse, ServiceSuccessResponse, ServiceUnhandledResponse}
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.model.requests.{DeviceDetailsGetRequest, DeviceDetailsRegisterRequest, DeviceDetailsRegistrationBadRequest, DeviceDetailsUnregisterRequest}
import com.flipcast.push.model.responses.{DeviceDetailsRegisterResponse, DeviceDetailsUnregisterFailureResponse, DeviceDetailsUnregisterSuccessResponse}
import spray.json.{JsObject, JsonParser}

class DeviceManagementHttpService (implicit val context: akka.actor.ActorRefFactory, implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = DeviceManagementHttpServiceWorker

  val deviceManagement = path("flipcast" / "device" / Segment / Segment / Segment ) { (configName: String, filterKeys: String, filterValues: String) =>
    get { ctx =>
      implicit val reqCtx = ctx
      val keys = filterKeys.split(",")
      val values = filterValues.split(",")
      val selectKeys = List.range(0, keys.length).map( i => keys(i) -> values(i)).toMap
      worker.execute(ServiceRequest[DeviceDetailsGetRequest](DeviceDetailsGetRequest(configName, selectKeys)))
    } ~
    delete { ctx =>
      implicit val reqCtx = ctx
      val keys = filterKeys.split(",")
      val values = filterValues.split(",")
      val selectKeys = List.range(0, keys.length).map( i => keys(i) -> values(i)).toMap
      worker.execute(ServiceRequest[DeviceDetailsUnregisterRequest](DeviceDetailsUnregisterRequest(configName, selectKeys)))
    }
  } ~
  path("flipcast" / "device" / Segment / Segment / Segment) { (configName: String, filterKeys: String, filterValues: String) =>
    (post | put) { ctx =>
      implicit val reqCtx = ctx
      val deviceData : Either[JsObject, Exception] = try {
          Left(JsonParser(ctx.request.entity.asString).asJsObject)
        } catch {
          case ex: Exception => Right(ex)
        }
      deviceData.isLeft match {
        case true =>
          val keys = filterKeys.split(",")
          val values = filterValues.split(",")
          val filter = List.range(0, keys.length).map( i => keys(i) -> values(i)).toMap
          worker.execute(ServiceRequest[DeviceDetailsRegisterRequest](DeviceDetailsRegisterRequest(configName, deviceData.left.get, filter)))
        case _ =>
          worker.execute(ServiceRequest[DeviceDetailsRegistrationBadRequest](DeviceDetailsRegistrationBadRequest(deviceData.right.get.getMessage)))
      }
    }
  }
}

object DeviceManagementHttpServiceWorker extends BaseHttpServiceWorker {

  def process[T](data: T) = {
    data match {
      case request: DeviceDetailsGetRequest =>
        val deviceData = DeviceDataSourceManager.dataSource(request.configName).get(request.configName, request.filter)
        deviceData match {
          case Some(d) => ServiceSuccessResponse[DeviceDetailsRegisterResponse](DeviceDetailsRegisterResponse(d))
          case _ => ServiceNotFoundResponse("Device not found for: " +request.filter.map( f => f._1 +"->" +f._2).mkString(" / "))
        }
      case request: DeviceDetailsUnregisterRequest =>
        val result = DeviceDataSourceManager.dataSource(request.configName).unregister(request.configName, request.filter)
        result match {
          case true =>
            ServiceSuccessResponse[DeviceDetailsUnregisterSuccessResponse](DeviceDetailsUnregisterSuccessResponse())
          case false =>
            ServiceSuccessResponse[DeviceDetailsUnregisterFailureResponse](DeviceDetailsUnregisterFailureResponse())
        }
      case request: DeviceDetailsRegisterRequest =>
        val result = DeviceDataSourceManager.dataSource(request.configName).register(request.configName,
          request.deviceData.compactPrint, request.filter)
        ServiceSuccessResponse[DeviceDetailsRegisterResponse](DeviceDetailsRegisterResponse(result))
      case request: DeviceDetailsRegistrationBadRequest =>
          ServiceBadRequestResponse(request.message)
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}
