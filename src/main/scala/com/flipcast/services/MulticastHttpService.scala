package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceActor, BaseHttpService}
import com.flipcast.push.protocol.{FlipcastPushProtocol, PushMessageProtocol}
import akka.actor.ActorRef
import com.flipcast.push.config.QueueConfigurationManager
import com.flipcast.rmq.ConnectionHelper
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.model.PushMessage
import com.flipcast.model.responses._
import com.github.sstone.amqp.Amqp.Publish
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.model.responses.ServiceBadRequestResponse
import spray.json._
import com.flipcast.model.requests.{ServiceRequest, BulkMessageRequest, MulticastRequest}
import com.flipcast.Flipcast
import com.flipcast.protocol.BulkMessageRequestProtocol

/**
 * HTTP service for multicast push requests
 *
 * @author Phaneesh Nagaraja
 */
class MulticastHttpService (implicit val context: akka.actor.ActorRefFactory,
                            implicit val serviceRegistry: ServiceRegistry)
                            extends BaseHttpService with PushMessageProtocol {

  def actorRefFactory = context

  def worker = serviceRegistry.actor("multicastServiceWorker")

  val multicastRoute = path("flipcast" / "push" / "multicast" / Segment / Segment / Segment) {
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
          case true => worker ! ServiceRequest[MulticastRequest](MulticastRequest(configName, selectKeys, payload.left.get))
          case false => worker ! ServiceBadRequestResponse(payload.right.get.getMessage)
        }
      }
    }
  }

}


class MulticastHttpServiceWorker extends BaseHttpServiceActor with FlipcastPushProtocol with BulkMessageRequestProtocol {

  var senderChannel : ActorRef = null

  var senderSidelineChannel: ActorRef = null

  override def preStart() {
    val config = QueueConfigurationManager.bulkConfig()
    if(senderChannel == null) {
      senderChannel = ConnectionHelper.createProducer(config.inputQueueName, config.inputExchange)
    }
    if(senderSidelineChannel == null) {
      senderSidelineChannel = ConnectionHelper.createProducer(config.sidelineQueueName, config.sidelineExchange)
    }
  }

  def process[T](request: T) = {
    request match {
      case request: MulticastRequest =>
        val config = QueueConfigurationManager.bulkConfig()
        val deviceCount = DeviceDataSourceManager.dataSource(request.configName).count(request.configName, request.filter)
        val batchSize = Flipcast.serverConfig.bulkMessageBatchSize
        val batches = deviceCount > batchSize match {
          case true => deviceCount / batchSize
          case false => 1
        }
        var limit = 1
        List.range[Long](0, batches).foreach( batch => {
          val split = BulkMessageRequest(request.configName, request.filter, request.message, batch.toInt, limit * batchSize)
          senderChannel ! Publish(config.inputExchange, config.inputQueueName, split.toJson.compactPrint.getBytes,
            ConnectionHelper.messageProperties, mandatory = false,
            immediate = false)
          limit += 1
        })
        ServiceSuccessResponse[MulticastSuccessResponse](MulticastSuccessResponse(deviceCount, batches))
      case bad: ServiceBadRequestResponse =>
        log.info("Invalid request to worker!!:" +bad)
        bad
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}