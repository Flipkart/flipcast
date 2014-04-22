package com.flipcast.services

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.{FlipcastService, Flipcast}
import spray.http.StatusCodes
import com.flipcast.protocol.ServiceProtocolSupport
import com.flipcast.model.responses.PingServiceResponse

/**
 * Simple spec for testing ping service
 *
 * @author Phaneesh Nagaraja
 */
class PingHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
  with ServiceProtocolSupport {

  def actorRefFactory = system

  Flipcast.boot()

  "Ping Service" should {
    "Return Pong for GET requests" in {
      Get("/ping") ~> ping ~> check {
        status mustEqual StatusCodes.OK
        responseAs[PingServiceResponse].message mustEqual "Pong"
      }
    }
  }

}
