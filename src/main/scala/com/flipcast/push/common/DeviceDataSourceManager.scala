package com.flipcast.push.common

import java.util.concurrent.ConcurrentHashMap


/**
 * Device data source manager per config
 *
 * @author Phaneesh Nagaraja
 */
object DeviceDataSourceManager {

  /**
   * Map of all registered data sources
   */
  private val providerCache = new ConcurrentHashMap[String, DeviceDataSource]()


  /**
   * Register a data source for a particular configuration
   * @param configName name of configuration
   * @param dataSource DeviceDataSource instance
   */
  def register(configName: String, dataSource: DeviceDataSource) {
    providerCache.contains(configName) match {
      case true =>
        new IllegalArgumentException("Duplicate config name")
      case false =>
        providerCache.put(configName, dataSource)
    }
  }


  /**
   * Get the data source instance for a given configuration
   * @param configName Name of the configuration
   * @return DeviceDataSource instance
   */
  def dataSource(configName: String) : DeviceDataSource = {
    providerCache.contains(configName) match {
      case true => providerCache.get(configName)
      case false => providerCache.get("default")
    }
  }
}
