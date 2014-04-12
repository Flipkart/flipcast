package com.flipcast

import akka.event.slf4j.Logger
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http
import com.flipcast.services._
import com.flipcast.push.config.{PushConfigurationManager, MongoBasedPushConfigurationProvider, PushConfigurationProvider}
import com.flipcast.mongo.ConnectionHelper
import com.flipcast.hazelcast.HazelcastManager
import com.flipcast.push.service.{BulkMessageConsumer, PushMessageHistoryManager, DeviceIdAutoUpdateManager, DeviceHouseKeepingManager}
import com.flipcast.push.gcm.service.FlipcastGcmRequestConsumer
import com.flipcast.model.config.ServerConfig
import com.flipcast.model.config.MongoConfig
import com.flipcast.model.config.HazelcastConfig
import com.flipcast.model.config.RmqConfig
import com.flipcast.push.common.DeviceDataSourceManager
import com.flipcast.push.mongo.MongoDeviceDataSource
import com.flipcast.push.apns.service.FlipcastApnsRequestConsumer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.routing.Broadcast


/**
 * Flipcast Service app
 *
 * @author Phaneesh Nagaraja
 */

object Flipcast extends App {

  /**
   * Logger for Flipcast app
   */
  val log = Logger("Flipcast")

  /**
   * Host name that will be used to bind the server
   */
  lazy val hostname = try {
      InetAddress.getLocalHost.getHostName
    } catch {
    case ex: Exception =>
      log.warn("Unable to resolve hostname! Returning loopback address. The server will not be reachable from external hosts")
      "127.0.0.1"
    }

  /**
   * Load application configuration
   */
  lazy val config = ConfigFactory.load()

  /**
   * Actor system for flipcast service
   */
  implicit lazy val system = ActorSystem("flipcast", config)

  lazy val serverConfig = ServerConfig(config.getConfig("flipcast.config.server"))

  lazy val rmqConfig = RmqConfig(config.getConfig("flipcast.config.rmq"))

  implicit lazy val hazelcastConfig = HazelcastConfig(config.getConfig("flipcast.config.hazelcast"))

  implicit lazy val mongoConfig = MongoConfig(config.getConfig("flipcast.config.mongo"))

  lazy val router = system.actorOf(Props[FlipcastRouter], "flipcastRouter")

  lazy val serviceRegistry = new ServiceRegistry()

  //Register all the services
  registerServices()

  //Initialize database connection
  ConnectionHelper.init()

  //Initialize hazelcast cluster
  HazelcastManager.init()

  implicit val pushConfigurationProvider : PushConfigurationProvider = new MongoBasedPushConfigurationProvider()(config.getConfig("flipcast.config.push.mongo"))

  PushConfigurationManager.init()

  registerDataSources()

  var serviceState : ServiceState.Value = ServiceState.IN_ROTATION

  log.info("--------------------------------------------------------------------------")
  log.info("Flipcast Service startup.....")
  log.info("Host: " +hostname)
  log.info("Port: " +serverConfig.port)
  log.info("--------------------------------------------------------------------------")

  /**
   * Startup server
   */
  IO(Http) ! Http.Bind(router, hostname, port = serverConfig.port)

  /**
   * Register message consumers
   */
  registerAllMessageConsumers()


  def registerServices() {
    serviceRegistry.register[PingHttpServiceWorker]("pingServiceWorker")
    serviceRegistry.register[StatusHttpServiceWorker]("statusServiceWorker")
    serviceRegistry.register[DeviceManagementHttpServiceWorker]("deviceManagementServiceWorker", 4)

    serviceRegistry.register[UnicastHttpServiceWorker]("unicastServiceWorker", 4)
    serviceRegistry.register[MulticastHttpServiceWorker]("multicastServiceWorker", 2)
    serviceRegistry.register[BroadcastHttpServiceWorker]("broadcastServiceWorker", 1)

    serviceRegistry.register[PushConfigHttpServiceWorker]("pushConfigServiceWorker")

    //Auto update services for maintaining data sanity
    serviceRegistry.register[DeviceHouseKeepingManager]("deviceHouseKeepingManager", 4)
    serviceRegistry.register[DeviceIdAutoUpdateManager]("deviceIdAutoUpdateManager", 4)

    //maintain message histories
    serviceRegistry.register[PushMessageHistoryManager]("pushMessageHistoryManager", 4)

    //Message Consumers
    serviceRegistry.register[FlipcastGcmRequestConsumer]("gcmRequestConsumer")
    serviceRegistry.register[FlipcastApnsRequestConsumer]("apnsRequestConsumer")
    serviceRegistry.register[BulkMessageConsumer]("bulkMessageConsumer")

  }

  /**
   * Register all the data source providers
   */
  def registerDataSources() {
    DeviceDataSourceManager.register("default", MongoDeviceDataSource)
  }

  /**
   * Register all message consumers
   */
  def registerAllMessageConsumers() {
    log.info("Starting all message consumers....")
    system.scheduler.scheduleOnce(30 seconds, serviceRegistry.actor("gcmRequestConsumer"), true)
    system.scheduler.scheduleOnce(35 seconds, serviceRegistry.actor("apnsRequestConsumer"), true)
    system.scheduler.scheduleOnce(40 seconds, serviceRegistry.actor("bulkMessageConsumer"), true)
  }

}

/**
 * Service states
 * @author Phaneesh Nagaraja
 */
object ServiceState extends Enumeration {

  val IN_ROTATION, OUT_OF_ROTATION = Value

}