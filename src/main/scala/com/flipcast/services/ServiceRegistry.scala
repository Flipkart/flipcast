package com.flipcast.services

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit._

import akka.actor._
import akka.cluster.routing._
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.event.slf4j.Logger
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
 * Service registry for keeping all the service worker actors
 * This will make sure that we are not creating actors repeatedly
 * A single instance of this registry is used in Flipcast app object
 * @param system Actor system that should be used to create actors
 *
 * @author Phaneesh Nagaraja
 */
class ServiceRegistry (implicit val system: ActorSystem) {

  val log = Logger("ServiceRegistry")

  implicit val timeout: Timeout = Duration(60, SECONDS)

  /**
   * Map of all registered actors
   */
  private val serviceCache = new ConcurrentHashMap[String, ActorRef]()

  /**
   * Register a actor into the registry
   * @param name Name of the actor that needs ot be registered
   * @param instances Total no of actor instances
   * @tparam T Actor or any of the subclasses of actor
   */
  def register[T <: Actor : ClassTag](name: String, instances: Int = 1, dispatcher: String = "akka.actor.default-dispatcher", isLocal: Boolean = true) {
    serviceCache.containsKey(name) match {
      case true => throw new IllegalArgumentException("Duplicate service registration")
      case false =>
        val aRef = instances match {
          case 1 =>
            system.actorOf(Props[T].withDispatcher(dispatcher), name)
          case _ =>
            system.actorOf(
              ClusterRouterPool(AdaptiveLoadBalancingPool(
                SystemLoadAverageMetricsSelector), ClusterRouterPoolSettings(
                totalInstances = instances * 64, maxInstancesPerNode = instances,
                allowLocalRoutees = isLocal, useRole = None)
            ).props(Props[T]).withDispatcher(dispatcher), name)
        }
        ClusterReceptionistExtension(system).registerService(aRef)
        log.info("Registered Service: " +Await.result(system.actorSelection("/user/" +name).resolveOne(), timeout.duration))
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
    serviceCache.containsKey(name) match {
      case true => serviceCache.get(name)
      case false => throw new IllegalArgumentException("Invalid service! Service not registered: " +name)
    }
  }


  /**
   * Lookup for actors using akka actor system
   * @param name name of the actor under "user" guardian
   * @return ActorSelection
   */
  def actorLookup(name: String) = {
    system.actorSelection("/user/" +name)
  }

}