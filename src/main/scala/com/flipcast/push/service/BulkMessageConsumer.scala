package com.flipcast.push.service

import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.flipcast.model.requests.BulkMessageRequest
import com.flipcast.protocol.BulkMessageRequestProtocol
import com.flipcast.push.common.{DeviceDataSourceManager, FlipcastRequestConsumer, PushMessageTransformerRegistry}
import com.flipcast.push.config.QueueConfigurationManager
import com.flipcast.push.model.DeviceOperatingSystemType
import com.flipcast.push.model.requests.FlipcastPushRequest
import com.flipcast.push.protocol.FlipcastPushProtocol
import spray.json._

/**
 * Service that consumes bulk requests and batches the devices and routes it appropriate queues
 *
 * @author Phaneesh Nagaraja
 */
class BulkMessageConsumer extends FlipcastRequestConsumer[BulkMessageRequest]
                            with BulkMessageRequestProtocol
                            with FlipcastPushProtocol {

  override def configType() = "bulk"

  override def init() {

  }

  override def consume(request: BulkMessageRequest) =  {
    val deviceResponse = request.query.isEmpty match {
      case true =>
        DeviceDataSourceManager.dataSource(request.configName)
          .listAll(request.configName, request.start, request.end).groupBy( _.osName)
      case false =>
        DeviceDataSourceManager.dataSource(request.configName)
          .list(request.configName, request.query, request.start, request.end).groupBy( _.osName)
    }
    val messagePayload = PushMessageTransformerRegistry.transformer(request.configName)
      .transform(request.configName, request.message.message)
    deviceResponse.contains(DeviceOperatingSystemType.ANDROID) match {
      case true =>
        deviceResponse(DeviceOperatingSystemType.ANDROID).grouped(100).foreach( dList => {
          val deviceIds = dList.map( _.cloudMessagingId).toList
          val framedMessage = FlipcastPushRequest(request.configName, deviceIds,
            messagePayload.getPayload(DeviceOperatingSystemType.ANDROID).getOrElse("{}"), None, None)
          mediator ! Publish(QueueConfigurationManager.config("gcm").inputQueueName, framedMessage.toJson.compactPrint)
        })
      case false =>
        log.warn("No Android devices in batch for request: " +request)
    }
    deviceResponse.contains(DeviceOperatingSystemType.iOS) match {
      case true =>
        deviceResponse(DeviceOperatingSystemType.iOS).grouped(100).foreach( dList => {
          val deviceIds = dList.map( _.cloudMessagingId).toList
          val framedMessage = FlipcastPushRequest(request.configName, deviceIds,
            messagePayload.getPayload(DeviceOperatingSystemType.iOS).getOrElse("{}"), None, None)
          mediator ! Publish(QueueConfigurationManager.config("apns").inputQueueName, framedMessage.toJson.compactPrint)
        })
      case false =>
        log.warn("No iOs devices in batch for request: " +request)
    }
    deviceResponse.contains(DeviceOperatingSystemType.WindowsPhone) match {
      case true =>
        deviceResponse(DeviceOperatingSystemType.WindowsPhone).grouped(100).foreach( dList => {
          val deviceIds = dList.map( _.cloudMessagingId).toList
          val framedMessage = FlipcastPushRequest(request.configName, deviceIds,
            messagePayload.getPayload(DeviceOperatingSystemType.WindowsPhone).getOrElse("{}"), None, None)
          mediator ! Publish(QueueConfigurationManager.config("mpns").inputQueueName, framedMessage.toJson.compactPrint)
        })
      case false =>
        log.warn("No Windows Phone devices in batch for request: " +request)
    }
    true
  }
}
