package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceWorker, BaseHttpService}
import com.flipcast.model.requests._
import com.flipcast.push.protocol.PushConfigurationProtocol
import com.flipcast.model.responses._
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.model.responses.ServiceFailureResponse
import com.flipcast.model.responses.ServiceSuccessResponse
import com.flipcast.model.requests.ServiceRequest
import java.util.Date
import com.flipcast.push.common.DeviceDataSourceManager
import scala.concurrent.duration.Duration

/**
 * Service for extracting push history
 *
 * @author Phaneesh Nagaraja
 */
class PushHistoryHttpService (implicit val context: akka.actor.ActorRefFactory,
                                   implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = PushHistoryHttpServiceWorker

  val pushHistoryRoute = path("flipcast" / "push" / "history" / Segment) { configName: String =>
    get { ctx =>
        implicit val reqCtx = ctx
        worker.execute(ServiceRequest[GetAllPushHistoryRequest](GetAllPushHistoryRequest(configName, "1d")))
      }
    } ~
    path("flipcast" / "push" / "history" / Segment / Segment ) { (configName: String, from: String) =>
      get { ctx =>
        implicit val reqCtx = ctx
        worker.execute(ServiceRequest[GetAllPushHistoryRequest](GetAllPushHistoryRequest(configName, from)))
      }
    }
}

object PushHistoryHttpServiceWorker extends BaseHttpServiceWorker with PushConfigurationProtocol {

  def process[T](data: T) = {
    data match {
      case request: GetAllPushHistoryRequest =>
        try {
          val from = new Date(System.currentTimeMillis() - Duration(request.from).toMillis)
          val data = DeviceDataSourceManager.dataSource(request.configName).pushHistory(request.configName, from)
          ServiceSuccessResponse[GetAllPushHistoryResponse](GetAllPushHistoryResponse(request.configName, data))
        } catch {
          case ex: Exception => ServiceFailureResponse(ex)
        }
      case _ =>
        ServiceUnhandledResponse()
    }
  }

}
