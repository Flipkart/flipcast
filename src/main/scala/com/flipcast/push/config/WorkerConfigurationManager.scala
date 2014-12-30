package com.flipcast.push.config

import java.util.concurrent.ConcurrentHashMap

import com.flipcast.Flipcast
import collection.JavaConverters._

/**
 * Provides queue configuration for different push service providers
 *
 * @author Phaneesh Nagaraja
 */
object WorkerConfigurationManager {

  val workerConfigurationCache = new ConcurrentHashMap[String, WorkerConfig]()

  def configs() : List[String] = {
    Flipcast.config.getStringList("flipcast.config.queue.configs").asScala.toList
  }

  def config(configType: String) : WorkerConfig = {
    if(!workerConfigurationCache.containsKey(configType)) {
      val c = Flipcast.config.getConfig("flipcast.config.worker." + configType)
      val priorityTags = c.getStringList("priorityTags").asScala
      workerConfigurationCache.put(configType, WorkerConfig(
        configType,
        priorityTags.map( pt => {
          pt -> PriorityConfig(c.getInt(pt +".workerInstances"), String.format("%sMessageConsumer-%s", configType, pt),
            c.getString( pt +".sidelineWorkerName"))
        }).toMap))
    }
    workerConfigurationCache.get(configType)
  }

  def worker(configType: String, priorityTag: String) = {
    config(configType).priorityConfigs(priorityTag).workerName
  }

  def bulkConfig() = {
    config("bulk")
  }

}
