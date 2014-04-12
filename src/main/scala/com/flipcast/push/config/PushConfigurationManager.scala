package com.flipcast.push.config

import akka.event.slf4j.Logger
import com.hazelcast.core.IMap
import com.flipcast.hazelcast.HazelcastManager
import collection.JavaConverters._

/**
 * Manager for push configuration
 *
 * @author Phaneesh Nagaraja
 */
object PushConfigurationManager {

  var pushConfigurationProvider : PushConfigurationProvider = null

  val log = Logger("PushConfigurationManager")

  lazy val configurationCache: IMap[String, PushConfig] = HazelcastManager.map[String, PushConfig]("PushConfigurationCache")


  def init() (implicit provider: PushConfigurationProvider) {
    val start = System.currentTimeMillis()
    log.info("Loading push configuration...")
    pushConfigurationProvider = provider
    val configs = provider.load()
    configs.foreach( c => {
      log.info("Loading configuration: " +c.configName)
      configurationCache.putIfAbsent(c.configName, c)
    })
    val end = System.currentTimeMillis()
    log.info("Loaded push configuration in " +(end - start) +" ms")
  }

  def save(config: PushConfig) = {
    val result = pushConfigurationProvider.save(config)
    configurationCache.put(config.configName, config)
    result
  }

  def config(configName: String) : Option[PushConfig] = {
    configurationCache.containsKey(configName) match {
      case true => Option(configurationCache.get(configName))
      case false => None
    }
  }

  def configs() : List[String] = {
    configurationCache.keySet().asScala.toList
  }

  def delete(configName: String) = {
    val result = pushConfigurationProvider.delete(configName)
    result match {
      case true =>
        configurationCache.delete(configName)
      case false => None
    }
    result
  }

}
