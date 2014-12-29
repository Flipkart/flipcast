package com.flipcast.services

import com.flipcast.Flipcast
import com.flipcast.common.{BaseHttpService, BaseHttpServiceWorker}
import com.flipcast.model.requests.{BroadcastRequest, BulkMessageRequest, ServiceRequest}
import com.flipcast.model.responses.{MulticastSuccessResponse, ServiceBadRequestResponse, ServiceSuccessResponse, ServiceUnhandledResponse}
import com.flipcast.protocol.BulkMessageRequestProtocol
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.config.QueueConfigurationManager
import com.flipcast.push.model.PushMessage
import com.flipcast.push.protocol.{FlipcastPushProtocol, PushMessageProtocol}
import spray.json._

/**
 * HTTP service for broadcast push requests
 *
 * @author Phaneesh Nagaraja
 */
class BroadcastHttpService (implicit val context: akka.actor.ActorRefFactory,
                            implicit val serviceRegistry: ServiceRegistry)
  extends BaseHttpService with PushMessageProtocol {

  def actorRefFactory = context

  def worker = BroadcastHttpServiceWorker

  val broadcastRoute = path("flipcast" / "push" / "broadcast" / Segment) { (configName: String) => {
      post { ctx =>
        implicit val reqCtx = ctx
        val payload = try {
          Left(JsonParser(ctx.request.entity.asString).convertTo[PushMessage])
        } catch {
          case ex: Exception =>
            log.error("Error converting message payload: ", ex)
            Right(ex)
        }
        payload.isLeft match {
          case true => worker.execute(ServiceRequest[BroadcastRequest](BroadcastRequest(configName, payload.left.get)))
          case false => worker.execute(ServiceBadRequestResponse(payload.right.get.getMessage))
        }
      }
    }
  }
}

object BroadcastHttpServiceWorker extends BaseHttpServiceWorker with FlipcastPushProtocol with BulkMessageRequestProtocol {

  def process[T](request: T) = {
    request match {
      case request: BroadcastRequest =>
        val config = QueueConfigurationManager.bulkConfig()
        val deviceCount = DeviceDataSourceManager.dataSource(request.configName).count(request.configName, Map.empty)
        val batchSize = Flipcast.serverConfig.bulkMessageBatchSize
        val batches = deviceCount > batchSize match {
          case true => deviceCount / batchSize
          case false => 1
        }
        var limit = 1
        List.range[Long](0, batches).foreach( batch => {
          val split = BulkMessageRequest(request.configName, Map.empty, request.message, batch.toInt, limit * batchSize)
          Flipcast.serviceRegistry.actorLookup(config.workerName) ! split
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
