package com.flipcast.rmq

import scala.collection.mutable
import com.github.sstone.amqp.{Amqp, RabbitMQConnection}
import java.util.concurrent.atomic.AtomicInteger
import com.flipcast.Flipcast
import akka.actor.{ActorRef, ActorSystem}
import com.rabbitmq.client.{AMQP, Address}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import com.github.sstone.amqp.Amqp._
import akka.event.slf4j.Logger
import com.github.sstone.amqp.Amqp.DeclareQueue
import com.github.sstone.amqp.Amqp.ChannelParameters
import com.github.sstone.amqp.Amqp.QueueParameters
import com.github.sstone.amqp.Amqp.DeclareExchange
import com.github.sstone.amqp.Amqp.QueueBind
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}

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
   * Keep track of open connections
   */
  val connections: mutable.HashMap[String, RabbitMQConnection] = new mutable.HashMap[String, RabbitMQConnection]() with mutable.SynchronizedMap[String, RabbitMQConnection]

  /**
   * Client properties that we need to set (in case of rmq cluster)
   */
  val clientProps = Map("x-ha-policy" -> "all")

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
    val name = "flipcast-%s".format(connectionCount.incrementAndGet())
    val reconnectDelay = Duration(Flipcast.rmqConfig.reconnectDelay, MILLISECONDS)
    val executor = Option(executorService)
    val addresses = Flipcast.rmqConfig.hosts.map( h => {
      val tokens = h.split(":")
      new Address(tokens(0), tokens(1).toInt)
    }).toArray
    val conn = new RabbitMQConnection(name = name,
      vhost = Flipcast.rmqConfig.vhost,
      user = Flipcast.rmqConfig.user,
      password = Flipcast.rmqConfig.pass,
      reconnectionDelay = reconnectDelay,
      executor = executor,
      addresses = Option(addresses))
    conn.waitForConnection.await()
    connections.put(name, conn)
    conn
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
    val channelParameters = Option(ChannelParameters(qos))
    val exchangeParams = ExchangeParameters(name = exchange, passive = false,
      exchangeType = "direct", durable = true, autodelete = false, clientProps)
    val connection = createConnection()
    val queueParams = QueueParameters(queueName, passive = false, durable = true, exclusive = false,
      autodelete = false, clientProps)
    val consumer = connection.createConsumer(exchangeParams, queueParams, queueName, listener,
      channelParams = channelParameters, autoack = false)
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
    val connection = createConnection()
    val queueParams = QueueParameters(queueName, passive = false, durable = true, exclusive = false, autodelete = false,
      clientProps)
    val producer = connection.createChannelOwner(channelParameters)
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
    connections.foreach( c => {
      log.info("Closing connection: " +c._1)
      c._2.stop
    })
  }

}
