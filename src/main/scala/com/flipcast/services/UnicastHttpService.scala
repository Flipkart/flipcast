package com.flipcast.services

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Send
import com.flipcast.common.{BaseHttpService, BaseHttpServiceActor}
import com.flipcast.model.requests.{ServiceRequest, UnicastRequest}
import com.flipcast.model.responses.{ServiceBadRequestResponse, ServiceNotFoundResponse, ServiceSuccessResponse, ServiceUnhandledResponse, _}
import com.flipcast.push.common.{DeviceDataSourceManager, PushMessageTransformerRegistry}
import com.flipcast.push.config.QueueConfigurationManager
import com.flipcast.push.model.requests.FlipcastPushRequest
import com.flipcast.push.model.{DeviceOperatingSystemType, PushMessage}
import com.flipcast.push.protocol.{FlipcastPushProtocol, PushMessageProtocol}
import spray.json._

/**
 * HTTP service for unicast push requests
 *
 * @author Phaneesh Nagaraja
 */
class UnicastHttpService (implicit val context: akka.actor.ActorRefFactory,
                          implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService
                          with PushMessageProtocol {

  def actorRefFactory = context

  def worker = serviceRegistry.actor("unicastServiceWorker")


  val unicastRoute = path("flipcast" / "push" / "unicast" / Segment / Segment / Segment) {
    (configName: String, filterKeys: String, filterValues: String) => {
      post { ctx =>
        implicit val reqCtx = ctx
        val keys = filterKeys.split(",")
        val values = filterValues.split(",")
        val selectKeys = List.range(0, keys.length).map( i => keys(i) -> values(i)).toMap
        val payload = try {
          Left(JsonParser(ctx.request.entity.asString).convertTo[PushMessage])
        } catch {
          case ex: Exception =>
            log.error("Error converting message payload: ", ex)
            Right(ex)
        }
        payload.isLeft match {
          case true => worker ! ServiceRequest[UnicastRequest](UnicastRequest(configName, selectKeys, payload.left.get))
          case false => worker ! ServiceBadRequestResponse(payload.right.get.getMessage)
        }
      }
    }
  }
}


class UnicastHttpServiceWorker extends BaseHttpServiceActor with FlipcastPushProtocol {

  val mediator = DistributedPubSubExtension(context.system).mediator


  def process[T](request: T) = {
    request match {
      case request: UnicastRequest =>
        val deviceResponse = DeviceDataSourceManager.dataSource(request.configName).get(request.configName, request.filter)
        deviceResponse match {
          case Some(device) =>
            val messagePayload = PushMessageTransformerRegistry.transformer(request.configName).transform(request.configName, request.message.message)
            device.osName match {
              case DeviceOperatingSystemType.ANDROID =>
                val framedMessage = FlipcastPushRequest(request.configName, List(device.cloudMessagingId),
                  messagePayload.getPayload(DeviceOperatingSystemType.ANDROID).getOrElse("{}"), None, None)
                mediator ! Send(QueueConfigurationManager.config("gcm").inputQueueName, framedMessage, localAffinity = false)
                ServiceSuccessResponse[UnicastSuccessResponse](UnicastSuccessResponse(device.deviceId, device.osName.toString))
              case DeviceOperatingSystemType.iOS =>
                val framedMessage = FlipcastPushRequest(request.configName, List(device.cloudMessagingId),
                  messagePayload.getPayload(DeviceOperatingSystemType.iOS).getOrElse("{}"), None, None)
                mediator ! Send(QueueConfigurationManager.config("apns").inputQueueName, framedMessage, localAffinity = false)
                ServiceSuccessResponse[UnicastSuccessResponse](UnicastSuccessResponse(device.deviceId, device.osName.toString))
              case DeviceOperatingSystemType.WindowsPhone =>
                val framedMessage = FlipcastPushRequest(request.configName, List(device.cloudMessagingId),
                  messagePayload.getPayload(DeviceOperatingSystemType.WindowsPhone).getOrElse("{}"), None, None)
                mediator ! Send(QueueConfigurationManager.config("mpns").inputQueueName, framedMessage, localAffinity = false)
                ServiceSuccessResponse[UnicastSuccessResponse](UnicastSuccessResponse(device.deviceId, device.osName.toString))
              case _ =>
                ServiceBadRequestResponse("Invalid device type: " +device.osName.toString)
            }
          case _ =>
            ServiceNotFoundResponse("Device not found for: " +request.filter.map( f => f._1 +"->" +f._2).mkString(" / "))
        }
      case bad: ServiceBadRequestResponse =>
        log.info("Invalid request to worker!!:" +bad)
        bad
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}