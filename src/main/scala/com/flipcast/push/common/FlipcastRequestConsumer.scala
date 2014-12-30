package com.flipcast.push.common

import java.util.concurrent.TimeUnit._
import java.util.{Date, UUID}

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import akka.util.Timeout
import com.flipcast.Flipcast
import com.flipcast.push.config.WorkerConfigurationManager
import com.flipcast.push.model.SidelinedMessage
import com.flipcast.push.model.requests.{FlipcastPushRequest, FlipcastRequest}
import com.flipcast.push.protocol.FlipcastPushProtocol
import spray.json._

import scala.concurrent.duration.Duration
import scala.reflect.{ClassTag, classTag}

/**
 * A simple message consumer actor for consuming RMQ messages
 *
 * @author Phaneesh Nagaraja
 */
abstract class  FlipcastRequestConsumer[T <: FlipcastRequest: ClassTag] extends Actor with FlipcastPushProtocol {

  implicit val timeout: Timeout = Duration(10, SECONDS)

  implicit val system: ActorSystem = Flipcast.system

  val DEFAULT_TIMEOUT = Duration(120, SECONDS)

  def configType() : String

  def consume(message: T) : Boolean

  def config = WorkerConfigurationManager.config(configType())

  def init()

  lazy val log = Logger(configType())

  override def preStart() {
    init()
    log.info("Starting message consumer on: " +config.configName +" Worker: " +self.path)
  }

  override def postStop(): Unit = {
    log.info("Stopping message consumer on: " +config.configName +" Worker: " +self.path)
  }

  def receive = {
    case message if classTag[T].runtimeClass.isInstance(message) =>
      try {
        consume(message.asInstanceOf[T]) match {
          case true =>

          case false =>
            sideline(message.asInstanceOf[T])
        }
      } catch {
        case ex: Exception =>
          log.error("Error sending notification", ex)
          sideline(message.asInstanceOf[T])
      }
  }

  private def sideline(message: T) {
    message match {
      case x: FlipcastPushRequest =>
        Flipcast.serviceRegistry.actorLookup(config.priorityConfigs(x.priority.getOrElse("default")).sidelineWorkerName) !
          SidelinedMessage(UUID.randomUUID().toString,
            x.configName, configType(), x.toJson.compactPrint, new Date())
    }
  }

  def resend(message: T) {
    message match {
      case x: FlipcastPushRequest =>
        Flipcast.serviceRegistry.actorLookup(config.priorityConfigs(x.priority.getOrElse("default")).workerName) ! message
    }
  }
}
