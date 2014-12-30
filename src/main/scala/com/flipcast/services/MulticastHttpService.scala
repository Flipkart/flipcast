package com.flipcast.services

import com.flipcast.Flipcast
import com.flipcast.common.{BaseHttpService, BaseHttpServiceWorker}
import com.flipcast.model.requests.{BulkMessageRequest, MulticastRequest, ServiceRequest}
import com.flipcast.model.responses.{ServiceBadRequestResponse, ServiceSuccessResponse, _}
import com.flipcast.protocol.BulkMessageRequestProtocol
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.config.WorkerConfigurationManager
import com.flipcast.push.model.PushMessage
import com.flipcast.push.protocol.{FlipcastPushProtocol, PushMessageProtocol}
import spray.json._

/**
 * HTTP service for multicast push requests
 *
 * @author Phaneesh Nagaraja
 */
class MulticastHttpService (implicit val context: akka.actor.ActorRefFactory,
                            implicit val serviceRegistry: ServiceRegistry)
                            extends BaseHttpService with PushMessageProtocol {

  def actorRefFactory = context

  def worker = MulticastHttpServiceWorker

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
          case true => worker.execute(ServiceRequest[MulticastRequest](MulticastRequest(configName, selectKeys, payload.left.get)))
          case false => worker.execute(ServiceBadRequestResponse(payload.right.get.getMessage))
        }
      }
    }
  }

}


object MulticastHttpServiceWorker extends BaseHttpServiceWorker with FlipcastPushProtocol with BulkMessageRequestProtocol {

  def process[T](request: T) = {
    request match {
      case request: MulticastRequest =>
        val config = WorkerConfigurationManager.bulkConfig()
        val deviceCount = DeviceDataSourceManager.dataSource(request.configName).count(request.configName, request.filter)
        val batchSize = Flipcast.serverConfig.bulkMessageBatchSize
        val batches = deviceCount > batchSize match {
          case true => deviceCount / batchSize
          case false => 1
        }
        var limit = 1
        List.range[Long](0, batches).foreach( batch => {
          val split = BulkMessageRequest(request.configName, request.filter, request.message, batch.toInt, limit * batchSize)
          Flipcast.serviceRegistry.actorLookup(config.priorityConfigs(request.message.priority.getOrElse("default")).workerName) ! split
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