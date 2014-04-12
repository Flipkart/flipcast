package com.flipcast.push.common

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.util.Timeout
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import akka.event.slf4j.Logger
import com.github.sstone.amqp.Amqp.{Publish, Reject, Ack, Delivery}
import com.flipcast.rmq.ConnectionHelper
import com.flipcast.Flipcast
import com.flipcast.push.config.QueueConfigurationManager

/**
 * A simple message consumer actor for consuming RMQ messages
 *
 * @author Phaneesh Nagaraja
 */
trait  FlipcastRequestConsumer extends Actor {

  implicit val timeout: Timeout = Duration(10, SECONDS)

  implicit val system: ActorSystem = Flipcast.system

  val log = Logger(this.getClass.getSimpleName)

  val DEFAULT_TIMEOUT = Duration(120, SECONDS)

  var sidelineChannel: ActorRef = null

  var consumerRef: ActorRef = null

  def configType() : String

  def basicQos() = QueueConfigurationManager.config(configType()).qos

  def consume(message: String) : Boolean

  def config = QueueConfigurationManager.config(configType())

  def init()

  override def preStart() {
    if(sidelineChannel == null) {
      sidelineChannel = ConnectionHelper.createProducer(config.sidelineQueueName, config.sidelineExchange)
    }
    if(consumerRef == null) {
      consumerRef = ConnectionHelper.createConsumer(config.inputQueueName, config.inputExchange, self, basicQos())
    }
    init()
    log.info("Starting message consumer on: " +config.inputExchange +"/" +config.inputQueueName)
  }

  def receive = {
    case true =>
      sender ! true
    case Delivery(consumerTag, envelope, properties, body) =>
      try {
        consume(new String(body)) match {
          case true =>
            sender ! Ack(envelope.getDeliveryTag)
          case false =>
            sender ! Reject(envelope.getDeliveryTag, requeue = false)
            sideline(body)
        }
      } catch {
        case ex: Exception =>
          log.error("Error sending notification", ex)
          sender ! Reject(envelope.getDeliveryTag, requeue = false)
          sideline(body)
      }
  }

  private def sideline(message: Array[Byte]) {
    sidelineChannel ! Publish(config.sidelineExchange, config.sidelineQueueName, message, ConnectionHelper.messageProperties, mandatory = false,
      immediate = false)
  }

}
