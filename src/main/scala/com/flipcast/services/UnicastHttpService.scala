package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceActor, BaseHttpService}
import com.flipcast.model.responses._
import com.flipcast.push.common.{PushMessageTransformerRegistry, DeviceDataSourceManager}
import com.flipcast.push.model.{PushMessage, DeviceOperatingSystemType}
import akka.actor.ActorRef
import com.flipcast.rmq.ConnectionHelper
import com.flipcast.push.config.QueueConfigurationManager
import scala.collection._
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.github.sstone.amqp.Amqp.Publish
import com.flipcast.model.responses.ServiceNotFoundResponse
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.model.requests.ServiceRequest
import com.flipcast.model.responses.ServiceBadRequestResponse
import com.flipcast.model.requests.UnicastRequest
import scala.Some
import com.flipcast.push.protocol.{PushMessageProtocol, FlipcastPushProtocol}
import com.flipcast.push.model.requests.FlipcastPushRequest
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

  val senderChannel = new mutable.HashMap[String, ActorRef]()

  val senderSidelineChannel = new mutable.HashMap[String, ActorRef]()

  override def preStart() {
    if(senderChannel.size == 0) {
      QueueConfigurationManager.configs().foreach(c => {
        val config = QueueConfigurationManager.config(c)
        senderChannel += c -> ConnectionHelper.createProducer(config.inputQueueName, config.inputExchange)
      })
    }
    if(senderSidelineChannel.size == 0) {
      QueueConfigurationManager.configs().foreach(c => {
        val config = QueueConfigurationManager.config(c)
        senderSidelineChannel += c -> ConnectionHelper.createProducer(config.sidelineQueueName, config.sidelineExchange)
      })
    }
  }

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
                senderChannel("gcm") ! Publish(QueueConfigurationManager.config("gcm").inputExchange,
                  QueueConfigurationManager.config("gcm").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
                  ConnectionHelper.messageProperties, mandatory = false,
                  immediate = false)
                ServiceSuccessResponse[UnicastSuccessResponse](UnicastSuccessResponse(device.deviceId, device.osName.toString))
              case DeviceOperatingSystemType.iOS =>
                val framedMessage = FlipcastPushRequest(request.configName, List(device.cloudMessagingId),
                  messagePayload.getPayload(DeviceOperatingSystemType.iOS).getOrElse("{}"), None, None)
                senderChannel("apns") ! Publish(QueueConfigurationManager.config("apns").inputExchange,
                  QueueConfigurationManager.config("apns").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
                  ConnectionHelper.messageProperties, mandatory = false,
                  immediate = false)
                ServiceSuccessResponse[UnicastSuccessResponse](UnicastSuccessResponse(device.deviceId, device.osName.toString))
              case DeviceOperatingSystemType.WindowsPhone =>
                val framedMessage = FlipcastPushRequest(request.configName, List(device.cloudMessagingId),
                  messagePayload.getPayload(DeviceOperatingSystemType.WindowsPhone).getOrElse("{}"), None, None)
                senderChannel("mpns") ! Publish(QueueConfigurationManager.config("mpns").inputExchange,
                  QueueConfigurationManager.config("mpns").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
                  ConnectionHelper.messageProperties, mandatory = false,
                  immediate = false)
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