package com.flipcast.push.config

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.event.slf4j.Logger
import com.flipcast.Flipcast
import com.google.common.cache.CacheBuilder

import scala.collection.JavaConverters._

/**
 * Manager for push configuration
 *
 * @author Phaneesh Nagaraja
 */
object PushConfigurationManager {

  var pushConfigurationProvider : PushConfigurationProvider = null

  val log = Logger("PushConfigurationManager")

  private val cache = CacheBuilder.newBuilder()
    .maximumSize(1024)
    .initialCapacity(8)
    .build[String, PushConfig]()

  val mediator = DistributedPubSubExtension(Flipcast.system).mediator

  def init() (implicit provider: PushConfigurationProvider) {
    val start = System.currentTimeMillis()
    log.info("Loading push configuration...")
    pushConfigurationProvider = provider
    val configs = provider.load()
    configs.foreach( c => {
      log.info("Loading configuration: " +c.configName)
      cache.asMap().putIfAbsent(c.configName, c)
    })
    val end = System.currentTimeMillis()
    log.info("Loaded push configuration in " +(end - start) +" ms")
  }

  def save(config: PushConfig) = {
    val result = pushConfigurationProvider.save(config)
    mediator ! Publish("pushConfig", PushConfigUpdatedMessage(config.configName, config))
    result
  }

  def config(configName: String) : Option[PushConfig] = {
    cache.asMap().containsKey(configName) match {
      case true => Option(cache.asMap().get(configName))
      case false => None
    }
  }

  def configs() : List[String] = {
    cache.asMap().keySet().asScala.toList
  }

  def delete(configName: String) = {
    val result = pushConfigurationProvider.delete(configName)
    result match {
      case true =>
        mediator ! Publish("pushConfig", PushConfigDeletedMessage(configName))
      case false => None
    }
    result
  }

  def updateConfig(config: PushConfig): Unit = {
    cache.put(config.configName, config)
  }

  def deleteConfig(configName: String): Unit = {
    cache.invalidate(configName)
  }

}
