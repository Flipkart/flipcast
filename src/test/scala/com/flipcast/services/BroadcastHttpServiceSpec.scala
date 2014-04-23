package com.flipcast.services

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.{Flipcast, FlipcastService}
import com.flipcast.protocol.ServiceProtocolSupport
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import spray.http.StatusCodes

/**
 * Simple spec for testing broadcast service
 *
 * @author Phaneesh Nagaraja
 */
class BroadcastHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
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

  "Broadcast Service" should {
    "Return 200 for sending notification to all registered devices" in {
      Post("/flipcast/push/broadcast/default", messagePayload) ~> broadcast ~> check {
        status mustEqual StatusCodes.OK
      }
    }
  }
}