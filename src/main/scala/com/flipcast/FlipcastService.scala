package com.flipcast

import spray.routing.HttpService
import com.flipcast.services._

/**
 * Flipcast Service route definition
 *
 * @author Phaneesh Nagaraja
 */
trait FlipcastService extends HttpService {

  implicit def executionContext = actorRefFactory.dispatcher

  implicit val serviceRegistry = Flipcast.serviceRegistry

  val ping = new PingHttpService().pingRoute

  val health = new StatusHttpService().statusRoute

  val config = new PushConfigHttpService().pushConfigRoute

  val device = new DeviceManagementHttpService().deviceManagement

  val unicast = new UnicastHttpService().unicastRoute

  val multicast = new MulticastHttpService().multicastRoute

  val broadcast = new BroadcastHttpService().broadcastRoute

  val pushHistory = new PushHistoryHttpService().pushHistoryRoute

}
