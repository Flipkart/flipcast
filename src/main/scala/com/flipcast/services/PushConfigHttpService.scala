package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceActor, BaseHttpService}
import com.flipcast.model.responses._
import com.flipcast.push.config.PushConfigurationManager
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.push.config.PushConfig
import com.flipcast.model.requests.{DeletePushConfigRequest, UpdatePushConfigRequest, GetPushConfigRequest, ServiceRequest}
import scala.Some
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.push.protocol.PushConfigurationProtocol
import spray.json.JsonParser

/**
 * Push configuration service
 *
 * @author Phaneesh Nagaraja
 */
class PushConfigHttpService (implicit val context: akka.actor.ActorRefFactory,
                             implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = serviceRegistry.actor("pushConfigServiceWorker")

  val pushConfigRoute = path("flipcast" / "push" / "config" / Segment) { configName: String =>
    get { ctx =>
      implicit val reqCtx = ctx
      worker ! ServiceRequest[GetPushConfigRequest](GetPushConfigRequest(configName))
    } ~
    delete  { ctx =>
      implicit val reqCtx = ctx
      worker ! ServiceRequest[DeletePushConfigRequest](DeletePushConfigRequest(configName))
    }
  } ~
  path("flipcast" / "push" / "config") {
    (put | post) { ctx =>
      implicit val reqCtx = ctx
      worker ! ServiceRequest[UpdatePushConfigRequest](UpdatePushConfigRequest(ctx))
    }
  }
}



class PushConfigHttpServiceWorker extends BaseHttpServiceActor with PushConfigurationProtocol {

  def process[T](data: T) = {
    data match {
      case request: GetPushConfigRequest =>
        try {
          PushConfigurationManager.config(request.configName) match {
            case Some(config) => ServiceSuccessResponse[PushConfig](config)
            case _ => ServiceNotFoundResponse(request.configName +" not found")
          }
        } catch {
          case ex: Exception => ServiceFailureResponse(ex)
        }
      case request: UpdatePushConfigRequest =>
        try {
            val pushConfig = JsonParser(request.ctx.request.entity.asString).convertTo[PushConfig]
            ServiceSuccessResponse[UpdatePushConfigResponse](UpdatePushConfigResponse(PushConfigurationManager.save(pushConfig)))
        } catch {
          case ex: Exception =>
            ServiceFailureResponse(ex)
        }
      case request: DeletePushConfigRequest =>
        ServiceSuccessResponse[UpdatePushConfigResponse](UpdatePushConfigResponse(PushConfigurationManager.delete(request.configName)))
      case _ =>
        ServiceUnhandledResponse()
    }
  }

}
