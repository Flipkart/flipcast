package com.flipcast.rmq

import com.github.sstone.amqp.{ChannelOwner, Consumer, ConnectionOwner, Amqp}
import java.util.concurrent.atomic.AtomicInteger
import com.flipcast.Flipcast
import akka.actor.{PoisonPill, ActorRef, ActorSystem}
import com.rabbitmq.client.{ConnectionFactory, AMQP, Address}
import com.github.sstone.amqp.Amqp._
import akka.event.slf4j.Logger
import com.github.sstone.amqp.Amqp.DeclareQueue
import com.github.sstone.amqp.Amqp.ChannelParameters
import com.github.sstone.amqp.Amqp.QueueParameters
import com.github.sstone.amqp.Amqp.DeclareExchange
import com.github.sstone.amqp.Amqp.QueueBind
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}
import scala.concurrent.duration._
import collection.JavaConverters._

/**
 * RabbitMQ connection helper
 *
 * @author Phaneesh Nagaraja
 */
object ConnectionHelper {

  private val executorService = new ThreadPoolExecutor(Runtime.getRuntime.availableProcessors(),
    Runtime.getRuntime.availableProcessors() * 2, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable](1024), new ThreadPoolExecutor.CallerRunsPolicy())

  val messageProperties = Option(new AMQP.BasicProperties.Builder()
    .contentType("application/json")
    .deliveryMode(2)
    .priority(1)
    .contentEncoding("UTF-8")
    .build())


  /**
   * RMQ Connection
   */
  var rmqConnection: ActorRef = null

  /**
   * Client properties that we need to set (in case of rmq cluster)
   */
  val clientProps = Map[String, AnyRef]("ha-mode" -> "all", "x-ha-policy" -> "all", "x-priority" -> new Integer(10))

  /**
   * Keep a count of connection
   */
  val connectionCount = new AtomicInteger(0)

  val log = Logger("RabbitMQConnectionHelper")

  /**
   * Create a connection so that we can create a consumer / producer
   * @param system actor system that needs to be used to create the connection
   * @return A RabbitMQ connection
   */
  private def createConnection() (implicit system: ActorSystem) = {
    if(rmqConnection == null) {
      val connectionFactory =  new ConnectionFactory()
      connectionFactory.setClientProperties(clientProps.asJava)
      connectionFactory.setAutomaticRecoveryEnabled(true)
      connectionFactory.setNetworkRecoveryInterval(10)
      val addresses = Flipcast.rmqConfig.hosts.map( h => {
        val tokens = h.split(":")
        new Address(tokens(0), tokens(1).toInt)
      }).toArray
      rmqConnection = system.actorOf(ConnectionOwner.props(connectionFactory, 1 second, addresses = Option(addresses), executor = Option(executorService)))
    }
    rmqConnection
  }

  /**
   * Create a consumer
   * @param queueName name of the queue
   * @param exchange name of the exchange
   * @param listener ActorRef of listener
   * @param system actor system that needs to be used to create the connection
   * @return ActorRef of consumer
   */
  def createConsumer(queueName: String, exchange: String, listener: ActorRef, qos: Int) (implicit system: ActorSystem) = {
    val exchangeParams = ExchangeParameters(name = exchange, passive = false,
      exchangeType = "direct", durable = true, autodelete = false, clientProps)
    val cParams = Option(ChannelParameters(qos))
    val queueParams = QueueParameters(queueName, passive = false, durable = true, exclusive = false, autodelete = false, clientProps)
    val connection = createConnection()
    val consumer = ConnectionOwner.createChildActor(connection, Consumer.props(listener, exchangeParams,queueParams, queueName,
      cParams, autoack = false))
    Amqp.waitForConnection(system, consumer).await()
    consumer ! DeclareExchange(exchangeParams)
    consumer ! DeclareQueue(queueParams)
    consumer ! QueueBind(queue = queueName, exchange = exchange, routing_key = queueName)
    consumer ! AddQueue(queueParams)
    consumer
  }

  /**
   * Create a producer and bind it to a exchange and a queue
   * @param queueName name of the queue
   * @param exchange name of the exchange
   * @param system actor system that needs to be used to create the connection
   * @return ActorRef to a producer
   */
  def createProducer(queueName: String, exchange: String) (implicit system: ActorSystem) = {
    val channelParameters = Option(ChannelParameters(1))
    val exchangeParams = ExchangeParameters(name = exchange, passive = false,
      exchangeType = "direct", durable = true, autodelete = false, clientProps)
    val queueParams = QueueParameters(queueName, passive = false, durable = true, exclusive = false, autodelete = false,
      clientProps)
    val connection = createConnection()
    val producer = ConnectionOwner.createChildActor(connection, ChannelOwner.props(channelParams = channelParameters))
    Amqp.waitForConnection(system, producer).await()
    producer ! DeclareExchange(exchangeParams)
    producer ! DeclareQueue(queueParams)
    producer ! QueueBind(queue = queueName, exchange = exchange, routing_key = queueName)
    producer
  }

  /**
   * Close all connections
   */
  def stop() {
    if(rmqConnection != null) {
      rmqConnection ! PoisonPill
    }
  }

}
