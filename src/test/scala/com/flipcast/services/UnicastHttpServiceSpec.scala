package com.flipcast.services

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.{Flipcast, FlipcastService}
import com.flipcast.protocol.ServiceProtocolSupport
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import spray.http.StatusCodes

/**
 * Simple spec for testing unicast service
 *
 * @author Phaneesh Nagaraja
 */
class UnicastHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
  with ServiceProtocolSupport {

  sequential

  def actorRefFactory = system

  implicit val routeTestTimeout = RouteTestTimeout(Duration(60, SECONDS))

  Flipcast.boot()

  val messagePayload = """{
                            "message" :
                               {
                                  "message" : "Test Message",
                                  "title" : "Test"
                               },
                            "ttl" : 600,
                            "delayWhileIdle" : false
                         }"""

  "Unicast Service" should {
    "Return 200 for sending notification to a registered device" in {
      Post("/flipcast/push/unicast/default/deviceId/92F48BBA-22E9-4A07-A639-D25299BCAF55", messagePayload) ~> unicast ~> check {
        status mustEqual StatusCodes.OK
      }
    }
  }
}