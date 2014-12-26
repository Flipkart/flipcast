package com.flipcast.services

import com.flipcast.common.{BaseHttpServiceWorker, BaseHttpService}
import com.flipcast.{ServiceState, Flipcast}
import java.util.concurrent.locks.ReentrantLock
import com.flipcast.model.responses._
import com.flipcast.model.responses.ServiceUnhandledResponse
import com.flipcast.model.requests.InRotationServiceRequest
import com.flipcast.model.requests.OutOfRotationServiceRequest
import com.flipcast.model.responses.InRotationResponse
import com.flipcast.model.requests.StatusCheckRequest
import com.flipcast.model.requests.ServiceRequest
import com.flipcast.model.responses.StatusCheckResponse
import com.flipcast.model.responses.OutOfRotationResponse

/**
 * Flipcast Status service
 * Endpoints:
 * GET /status - Gets the status of the service (Used as health check for load balancer)
 * POST /inr - Brings service back online (Bring the service back into rotation on load balancer)
 * POST /oor - Takes the service offline (Take the service out of rotation on load balancer)
 * @author Phaneesh Nagaraja
 */
class StatusHttpService (implicit val context: akka.actor.ActorRefFactory,
                         implicit val serviceRegistry: ServiceRegistry) extends BaseHttpService {

  def actorRefFactory = context

  def worker = StatusHttpServiceWorker

  val statusRoute = path("status") {
    get { ctx =>
      implicit val reqCtx = ctx
      worker.execute(ServiceRequest[StatusCheckRequest](StatusCheckRequest()))
    }
  } ~
  path("inr") {
    post { ctx =>
      implicit val reqCtx = ctx
      worker.execute(InRotationServiceRequest())
    }
  } ~
  path("oor") {
    post { ctx =>
      implicit val reqCtx = ctx
      worker.execute(OutOfRotationServiceRequest())
    }
  }

}

/**
 * Status Service actor
 *
 * @author Phaneesh Nagaraja
 */
object StatusHttpServiceWorker extends BaseHttpServiceWorker {

  val lock = new ReentrantLock()

  def process[T](data: T) = {
    data match {
      case request: StatusCheckRequest =>
        ServiceSuccessResponse[StatusCheckResponse](StatusCheckResponse(Flipcast.serviceState == ServiceState.IN_ROTATION))
      case request: InRotationServiceRequest =>
        var result = false
        try {
          lock.tryLock() match {
            case true =>
              Flipcast.serviceState = ServiceState.IN_ROTATION
              result = true
              lock.unlock()
            case false => None
          }
        } catch {
          case ex: Exception =>
            log.warn("Failed to acquire status! Concurrent request error")
        }
        InRotationResponse(result)
      case request: OutOfRotationServiceRequest =>
        var result = false
        try {
          lock.tryLock() match {
            case true =>
              Flipcast.serviceState = ServiceState.OUT_OF_ROTATION
              result = true
              lock.unlock()
            case false => None
          }
        } catch {
          case ex: Exception =>
            log.warn("Failed to acquire status! Concurrent request error")
        }
        OutOfRotationResponse(result)
      case _ =>
        ServiceUnhandledResponse()
    }
  }
}

