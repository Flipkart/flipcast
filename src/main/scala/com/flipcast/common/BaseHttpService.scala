package com.flipcast.common

import spray.routing.HttpService
import com.flipcast.protocol.ServiceProtocolSupport
import org.slf4j.LoggerFactory
import com.flipcast.Flipcast
import akka.actor.ActorRef

/**
 * Base trait for all http services
 * Provides simple way to define a worker for handling the service
 *
 * @author Phaneesh Nagaraja
 */
trait BaseHttpService extends HttpService with ServiceProtocolSupport {

  val log = LoggerFactory.getLogger(this.getClass.getSimpleName)

  implicit val system = Flipcast.system

  def worker : BaseHttpServiceWorker

}
