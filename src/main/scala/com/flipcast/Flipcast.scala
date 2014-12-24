package com.flipcast

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.event.slf4j.Logger
import akka.io.IO
import com.codahale.metrics.{MetricFilter, Slf4jReporter}
import com.flipcast.cluster.FlipcastClusterListener
import com.flipcast.common.FlipCastMetricsRegistry
import com.flipcast.model.config.{HazelcastConfig, MongoConfig, RmqConfig, ServerConfig}
import com.flipcast.mongo.ConnectionHelper
import com.flipcast.push.apns.service.FlipcastApnsRequestConsumer
import com.flipcast.push.common.{FlipcastSidelineConsumer, DeviceDataSourceManager}
import com.flipcast.push.config._
import com.flipcast.push.gcm.service.FlipcastGcmRequestConsumer
import com.flipcast.push.mongo.MongoDeviceDataSource
import com.flipcast.push.mpns.service.FlipcastMpnsRequestConsumer
import com.flipcast.push.service.{BulkMessageConsumer, DeviceHouseKeepingManager, DeviceIdAutoUpdateManager, PushMessageHistoryManager}
import com.flipcast.services._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.can.Http


/**
 * Flipcast Service app
 *
 * @author Phaneesh Nagaraja
 */

object Flipcast extends App {

  /**
   * Logger for Flipcast app
   */
  lazy val log = Logger("flipcast")

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

  val clusterListener = system.actorOf(Props[FlipcastClusterListener], "flipcastClusterListener")

  lazy val serviceRegistry = new ServiceRegistry()

  val mediator = DistributedPubSubExtension(system).mediator

  boot()

  implicit val pushConfigurationProvider : PushConfigurationProvider = new MongoBasedPushConfigurationProvider()(config.getConfig("flipcast.config.push.mongo"))

  PushConfigurationManager.init()

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

  def registerServices() {
    //Ping Service
    serviceRegistry.register[PingHttpServiceWorker]("pingServiceWorker")
    //Status Service
    serviceRegistry.register[StatusHttpServiceWorker]("statusServiceWorker")
    //Device management Service
    serviceRegistry.register[DeviceManagementHttpServiceWorker]("deviceManagementServiceWorker", 4)
    //Push History fetch service
    serviceRegistry.register[PushHistoryHttpServiceWorker]("pushHistoryHttpServiceWorker")

    //Push Messaging API
    serviceRegistry.register[UnicastHttpServiceWorker]("unicastServiceWorker", 4)
    serviceRegistry.register[MulticastHttpServiceWorker]("multicastServiceWorker", 2)
    serviceRegistry.register[BroadcastHttpServiceWorker]("broadcastServiceWorker", 1)

    //Configuration Service
    serviceRegistry.register[PushConfigHttpServiceWorker]("pushConfigServiceWorker")

    //Auto update services for maintaining data sanity
    serviceRegistry.register[DeviceHouseKeepingManager]("deviceHouseKeepingManager", 4)
    serviceRegistry.register[DeviceIdAutoUpdateManager]("deviceIdAutoUpdateManager", 4)

    //maintain message histories
    serviceRegistry.register[PushMessageHistoryManager]("pushMessageHistoryManager", 4)

    //Message Consumers

    serviceRegistry.register[FlipcastGcmRequestConsumer]("gcmRequestConsumer", dispatcher = Option("akka.actor.gcm-dispatcher"))
    serviceRegistry.register[FlipcastApnsRequestConsumer]("apnsRequestConsumer", dispatcher = Option("akka.actor.apns-dispatcher"))
    serviceRegistry.register[FlipcastMpnsRequestConsumer]("mpnsRequestConsumer", dispatcher = Option("akka.actor.mpns-dispatcher"))
    serviceRegistry.register[BulkMessageConsumer]("bulkMessageConsumer")

    //Sideline message consumer which will persist any abandoned/sidelined message
    serviceRegistry.register[FlipcastSidelineConsumer]("flipcastSidelineConsumer")

    //Push configuration change listener to auto update cache
    serviceRegistry.register[PushConfigChangeListener]("pushConfigChangeListener")
  }

  /**
   * Register all the data source providers
   */
  def registerDataSources() {
    DeviceDataSourceManager.register("default", MongoDeviceDataSource)
  }

  def startMetrics() {
    FlipCastMetricsRegistry.registerDefaults()
    Slf4jReporter.forRegistry(FlipCastMetricsRegistry.metrics)
      .outputTo(LoggerFactory.getLogger("metrics"))
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build()
      .start(1, TimeUnit.MINUTES)
  }


  def boot() {

    //Register all the services
    registerServices()


    //Initialize database connection
    ConnectionHelper.init()

    //Register datasource
    registerDataSources()

    //Set service instance to active state
    serviceState = ServiceState.IN_ROTATION
  }

}

/**
 * Service states
 * @author Phaneesh Nagaraja
 */
object ServiceState extends Enumeration {

  val IN_ROTATION, OUT_OF_ROTATION = Value

}