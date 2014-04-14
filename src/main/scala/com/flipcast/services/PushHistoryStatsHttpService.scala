package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceActor, BaseHttpService}
import com.flipcast.model.requests._
import com.flipcast.push.protocol.PushConfigurationProtocol
import com.flipcast.push.config.{PushConfig, PushConfigurationManager}
import com.flipcast.model.responses._
import spray.json.JsonParser
import com.flipcast.push.config.PushConfig
import com.flipcast.model.responses.ServiceFailureResponse
import com.flipcast.model.responses.ServiceNotFoundResponse
import com.flipcast.model.responses.ServiceSuccessResponse
import scala.Some
import com.flipcast.model.responses.UpdatePushConfigResponse
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.push.config.PushConfig
import com.flipcast.model.responses.ServiceFailureResponse
import com.flipcast.model.responses.ServiceNotFoundResponse
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.model.requests.ServiceRequest
import com.flipcast.model.requests.GetPushConfigRequest
import scala.Some
import com.flipcast.model.requests.UpdatePushConfigRequest
import com.flipcast.model.requests.DeletePushConfigRequest
import com.flipcast.model.responses.UpdatePushConfigResponse

/**
 * Service for extracting push history
 *
 * @author
 */
class PushHistoryStatsHttpService (implicit val context: akka.actor.ActorRefFactory,
                                   implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = serviceRegistry.actor("pushHistoryServiceWorker")

  val pushHistoryRoute = path("flipcast" / "push" / "history" / Segment) { configName: String =>
    get { ctx =>
        implicit val reqCtx = ctx
        worker ! ServiceRequest[GetAllPushHistoryRequest](GetAllPushHistoryRequest(configName))
      }
    }
}

class PushHistoryHttpServiceWorker extends BaseHttpServiceActor with PushConfigurationProtocol {

  def process[T](data: T) = {
    data match {
      case request: GetAllPushHistoryRequest =>
        try {
          PushConfigurationManager.config(request.configName) match {
            case Some(config) => ServiceSuccessResponse[PushConfig](config)
            case _ => ServiceNotFoundResponse(request.configName +" not found")
          }
        } catch {
          case ex: Exception => ServiceFailureResponse(ex)
        }
      case _ =>
        ServiceUnhandledResponse()
    }
  }

}
