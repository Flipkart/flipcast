package com.flipcast.services

import collection.mutable
import akka.actor.{Props, Actor, ActorSystem, ActorRef}
import scala.reflect.ClassTag
import akka.routing.RoundRobinRouter

/**
 * Service registry for keeping all the service worker actors
 * This will make sure that we are not creating actors repeatedly
 * A single instance of this registry is used in Flipcast app object
 * @param system Actor system that should be used to create actors
 *
 * @author Phaneesh Nagaraja
 */
class ServiceRegistry (implicit val system: ActorSystem) {

  /**
   * Map of all registered actors
   */
  private val serviceCache = new mutable.HashMap[String, ActorRef]() with mutable.SynchronizedMap[String, ActorRef]

  /**
   * Register a actor into the registry
   * @param name Name of the actor that needs ot be registered
   * @param instances Total no of actor instances
   * @tparam T Actor or any of the subclasses of actor
   */
  def register[T <: Actor : ClassTag](name: String, instances: Int = 1) {
    serviceCache.contains(name) match {
      case true => throw new IllegalArgumentException("Duplicate service registration")
      case false =>
        val aRef = instances match {
          case 1 =>
            system.actorOf(Props[T], name)
          case _ =>
            system.actorOf(Props[T].withRouter(RoundRobinRouter(nrOfInstances = instances)), name)
        }
        serviceCache.put(name, aRef)
    }
  }

  /**
   * Lookup method for fetching back service worker actor
   * @param name of the service actor
   * @return ActorRef for service worker
   * @throws IllegalArgumentException when a invalid name is supplied
   */
  def actor(name: String) = {
    serviceCache.contains(name) match {
      case true => serviceCache(name)
      case false => throw new IllegalArgumentException("Invalid service! Service not registered")
    }
  }
}