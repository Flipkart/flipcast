package com.flipcast.push.common

import java.util.{Date, UUID}
import java.util.concurrent.TimeUnit._

import akka.actor.{Actor, ActorSystem}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator._
import akka.event.slf4j.Logger
import akka.util.Timeout
import com.flipcast.Flipcast
import com.flipcast.push.config.QueueConfigurationManager
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

  val mediator = DistributedPubSubExtension(context.system).mediator

  def configType() : String

  def consume(message: T) : Boolean

  def config = QueueConfigurationManager.config(configType())

  def init()

  lazy val log = Logger(configType())

  override def preStart() {
    mediator ! Put(self)
    init()
    log.info("Starting message consumer on: " +config.inputQueueName +" Worker: " +self.path)
  }

  override def postStop(): Unit = {
    mediator ! Remove(self.path.toString)
    log.info("Stopping message consumer on: " +config.inputQueueName +" Worker: " +self.path)
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
        mediator ! Send(config.sidelineQueueName, SidelinedMessage(UUID.randomUUID().toString,
          x.configName, configType(), x.toJson.compactPrint, new Date()),localAffinity = false)
    }

  }

  def resend(message: T) {
    mediator ! Send(config.inputQueueName, message, localAffinity = false)
  }

}
