package com.flipcast.services

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import com.flipcast.{FlipcastService, Flipcast}
import spray.http.StatusCodes
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.protocol.ServiceProtocolSupport

/**
 * Simple spec for testing push config service
 *
 * @author Phaneesh Nagaraja
 */
class PushConfigHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
  with ServiceProtocolSupport {

  sequential

  def actorRefFactory = system

  implicit val routeTestTimeout = RouteTestTimeout(Duration(60, SECONDS))

  Flipcast.boot()

  val configPayload = """{
                           "configName" : "test",
                           "gcm" : {
                              "apiKey": "dummykey",
                              "defaultDelayWhileIdle": false,
                              "defaultExpiry": 600
                           },
                           "apns" : {
                              "certificate": "/path/to/dertificate",
                              "password": "password to your certificate",
                              "sandbox": false,
                              "defaultExpiry": 600
                           }
                        }"""

  "Push Config Service" should {
    "Return 200 for when a known configuration is fetched" in {
      Post("/flipcast/push/config", configPayload) ~> config ~> check {
        status mustEqual StatusCodes.OK
      }
      Get("/flipcast/push/config/test") ~> config ~> check {
        status mustEqual StatusCodes.OK
      }
    }
  }
}
