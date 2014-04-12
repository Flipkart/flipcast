package com.flipcast.push.service

import com.flipcast.push.common.{PushMessageTransformerRegistry, DeviceDataSourceManager, FlipcastRequestConsumer}

import com.flipcast.protocol.BulkMessageRequestProtocol
import com.flipcast.model.requests.BulkMessageRequest
import com.flipcast.push.model.DeviceOperatingSystemType
import scala.collection.mutable
import akka.actor.ActorRef
import com.flipcast.push.config.QueueConfigurationManager
import com.flipcast.rmq.ConnectionHelper
import com.flipcast.push.model.requests.FlipcastPushRequest
import com.github.sstone.amqp.Amqp.Publish
import spray.json._
import com.flipcast.push.protocol.FlipcastPushProtocol

/**
 * Service that consumes bulk requests and batches the devices and routes it appropriate queues
 *
 * @author Phaneesh Nagaraja
 */
class BulkMessageConsumer extends FlipcastRequestConsumer
                            with BulkMessageRequestProtocol
                            with FlipcastPushProtocol {

  override def configType() = "bulk"

  val senderChannel = new mutable.HashMap[String, ActorRef]()

  val senderSidelineChannel = new mutable.HashMap[String, ActorRef]()

  override def init() {
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

  override def consume(message: String) =  {
    val request = JsonParser(message).convertTo[BulkMessageRequest]
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
          senderChannel("gcm") ! Publish(QueueConfigurationManager.config("gcm").inputExchange,
            QueueConfigurationManager.config("gcm").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
            ConnectionHelper.messageProperties, mandatory = false,
            immediate = false)
        })
      case false =>
        log.warn("No Android devices in batch for request: " +message)
    }
    deviceResponse.contains(DeviceOperatingSystemType.iOS) match {
      case true =>
        deviceResponse(DeviceOperatingSystemType.iOS).grouped(100).foreach( dList => {
          val deviceIds = dList.map( _.cloudMessagingId).toList
          val framedMessage = FlipcastPushRequest(request.configName, deviceIds,
            messagePayload.getPayload(DeviceOperatingSystemType.iOS).getOrElse("{}"), None, None)
          senderChannel("apns") ! Publish(QueueConfigurationManager.config("apns").inputExchange,
            QueueConfigurationManager.config("apns").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
            ConnectionHelper.messageProperties, mandatory = false,
            immediate = false)
        })
      case false =>
        log.warn("No iOs devices in batch for request: " +message)
    }
    deviceResponse.contains(DeviceOperatingSystemType.WindowsPhone) match {
      case true =>
        deviceResponse(DeviceOperatingSystemType.WindowsPhone).grouped(100).foreach( dList => {
          val deviceIds = dList.map( _.cloudMessagingId).toList
          val framedMessage = FlipcastPushRequest(request.configName, deviceIds,
            messagePayload.getPayload(DeviceOperatingSystemType.WindowsPhone).getOrElse("{}"), None, None)
          senderChannel("mpns") ! Publish(QueueConfigurationManager.config("mpns").inputExchange,
            QueueConfigurationManager.config("mpns").inputQueueName, framedMessage.toJson.compactPrint.getBytes,
            ConnectionHelper.messageProperties, mandatory = false,
            immediate = false)
        })
      case false =>
        log.warn("No Windows Phone devices in batch for request: " +message)
    }
    true
  }
}
