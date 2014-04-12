package com.flipcast.push.config

import com.flipcast.Flipcast
import collection.JavaConverters._

/**
 * Provides queue configuration for different push service providers
 *
 * @author Phaneesh Nagaraja
 */
object QueueConfigurationManager {

  def configs() : List[String] = {
    Flipcast.config.getStringList("flipcast.config.queue.configs").asScala.toList
  }

  def config(configType: String) : QueueConfig = {
    val c = Flipcast.config.getConfig("flipcast.config.queue." + configType)
    QueueConfig(
      c.getString("inputQueueName"),
      c.getString("inputExchange"),
      c.getString("sidelineQueueName"),
      c.getString("sidelineExchange"),
      c.getInt("qos")
    )
  }

  def bulkConfig() = {
    config("bulk")
  }

}
