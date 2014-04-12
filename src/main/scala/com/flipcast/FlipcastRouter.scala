package com.flipcast

import akka.actor.Actor

/**
 * Flipcast Service main router
 *
 * @author Phaneesh Nagaraja
 */
class FlipcastRouter extends Actor with FlipcastService {

  def actorRefFactory = context

  def receive = runRoute(ping ~ health ~ config ~ device ~ unicast ~ multicast ~ broadcast)
}
