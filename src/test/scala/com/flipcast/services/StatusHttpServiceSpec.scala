package com.flipcast.services

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.{Flipcast, FlipcastService}
import com.flipcast.protocol.ServiceProtocolSupport
import spray.http.StatusCodes
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._

/**
 * Simple spec for testing status service
 *
 * @author Phaneesh Nagaraja
 */
class StatusHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
  with ServiceProtocolSupport {

  sequential

  def actorRefFactory = system

  implicit val routeTestTimeout = RouteTestTimeout(Duration(60, SECONDS))

  Flipcast.boot()

  "Status Service" should {
    "Return 200 for GET when in rotation" in {
      Get("/status") ~> health ~> check {
        status mustEqual StatusCodes.OK
      }
    }
    "Return 200 for putting out of rotation" in {
      Post("/oor") ~> health ~> check {
        status mustEqual StatusCodes.OK
      }
    }
    "Return 503 after putting the node out of rotation" in {
      Get("/status") ~> health ~> check {
        status mustEqual StatusCodes.ServiceUnavailable
      }
    }
    "Return 200 for putting in rotation" in {
      Post("/inr") ~> health ~> check {
        status mustEqual StatusCodes.OK
      }
    }
    "Return 200 for GET when in rotation" in {
      Get("/status") ~> health ~> check {
        status mustEqual StatusCodes.OK
      }
    }
  }



}
