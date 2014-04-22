package com.flipcast.services

import com.flipcast.common.{FlipCastMetricsRegistry, BaseHttpServiceActor, BaseHttpService}
import com.flipcast.model.requests.{ServiceRequest, PingServiceRequest}
import com.flipcast.model.responses.{ServiceSuccessResponse, ServiceUnhandledResponse, PingServiceResponse}

class PingHttpService (implicit val context: akka.actor.ActorRefFactory, implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = serviceRegistry.actor("pingServiceWorker")

  val pingRoute = path("ping") {
    get { ctx =>
       implicit val reqCtx = ctx
       worker ! ServiceRequest[PingServiceRequest](PingServiceRequest())
    }
  }
}

class PingHttpServiceWorker extends BaseHttpServiceActor {

  def process[T](data: T) = {
    data match {
      case request: PingServiceRequest =>
        ServiceSuccessResponse(PingServiceResponse())
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}
