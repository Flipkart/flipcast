package com.flipcast.services

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.flipcast.{Flipcast, FlipcastService}
import com.flipcast.protocol.ServiceProtocolSupport
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import spray.http.StatusCodes

/**
 * Simple spec for testing device management service
 *
 * @author Phaneesh Nagaraja
 */
class DeviceManagementHttpServiceSpec extends Specification with Specs2RouteTest with FlipcastService
  with ServiceProtocolSupport {

  sequential

  def actorRefFactory = system

  implicit val routeTestTimeout = RouteTestTimeout(Duration(60, SECONDS))

  Flipcast.boot()

  val devicePayload = """{
                        "deviceId" : "92F48BBA-22E9-4A07-A639-D25299BCAF44",
                        "cloudMessagingId" : "a4f0959f49a2edd3854abae2f1611024bf9803a8331b09d19f9f347aa487a436",
                        "osName" : "iOS",
                        "osVersion" : "7.1",
                        "brand" : "Apple",
                        "model" : "iPhone 5 GSM",
                        "appName" : "default",
                        "appVersion" : "1.0"
                        }"""

  "Device Management Service" should {
    "Return 200 for registering a device" in {
      Post("/flipcast/device/default/deviceId/92F48BBA-22E9-4A07-A639-D25299BCAF44", devicePayload) ~> device ~> check {
        status mustEqual StatusCodes.OK
      }
    }
    "Return 200 for fetching registered device" in {
      Get("/flipcast/device/default/deviceId/92F48BBA-22E9-4A07-A639-D25299BCAF44") ~> device ~> check {
        status mustEqual StatusCodes.OK
      }
    }
    "Return 200 for un-registering a device" in {
      Delete("/flipcast/device/default/deviceId/92F48BBA-22E9-4A07-A639-D25299BCAF44") ~> device ~> check {
        status mustEqual StatusCodes.OK
      }
    }
  }
}
