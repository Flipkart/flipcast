package com.flipcast.services

import com.flipcast.common.{BaseHttpService, BaseHttpServiceWorker}
import com.flipcast.model.requests.{PingServiceRequest, ServiceRequest}
import com.flipcast.model.responses.{PingServiceResponse, ServiceSuccessResponse, ServiceUnhandledResponse}

class PingHttpService (implicit val context: akka.actor.ActorRefFactory, implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = PingHttpServiceWorker

  val pingRoute = path("ping") {
    get { ctx =>
       implicit val reqCtx = ctx
       worker.execute(ServiceRequest[PingServiceRequest](PingServiceRequest()))
    }
  }
}

object PingHttpServiceWorker extends BaseHttpServiceWorker {

  def process[T](data: T) = {
    data match {
      case request: PingServiceRequest =>
        ServiceSuccessResponse(PingServiceResponse())
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}
