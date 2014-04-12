package com.flipcast.hazelcast

import com.hazelcast.core.{IMap, Hazelcast, HazelcastInstance}
import akka.event.slf4j.Logger
import com.hazelcast.config.{MapConfig, Config}
import java.net.{Inet6Address, NetworkInterface}
import com.flipcast.model.config.HazelcastConfig
import collection.JavaConverters._

/**
 * Hazelcast Manager
 *
 * @author Phaneesh Nagaraja
 */
object HazelcastManager {

  private var hazelcastInstance : HazelcastInstance = null

  private val log = Logger("DistributedCacheManager")

  def init() (implicit hazelcastConfig: HazelcastConfig) {
    if(hazelcastInstance == null) {
      log.info("Starting distributed cache manager...")
      val config = new Config()
      config.setInstanceName("flipcast")
      config.getGroupConfig.setName("flipcast").setPassword(hazelcastConfig.password)
      val networkConfig = config.getNetworkConfig
      networkConfig.setPort(hazelcastConfig.port)
      networkConfig.setPortAutoIncrement(false)
      val joinConfig = networkConfig.getJoin
      joinConfig.getMulticastConfig.setEnabled(false)
      log.info("-----------------------------------------------")
      log.info("Hosts:")
      log.info("-----------------------------------------------")
      hazelcastConfig.hosts.foreach( h => {
        log.info(h)
        joinConfig.getTcpIpConfig.addMember(h).setEnabled(true)
      })
      log.info("-----------------------------------------------")
      NetworkInterface.getNetworkInterfaces.asScala.foreach( ni => {
        ni.getInetAddresses.asScala.filter(a => !a.isLoopbackAddress && !a.isMulticastAddress
          && !a.isInstanceOf[Inet6Address]).foreach( a => {
          networkConfig.getInterfaces.addInterface(a.getHostAddress).setEnabled(true)
        })
      })
      log.info("Maps:")
      log.info("-----------------------------------------------")
      hazelcastConfig.maps.foreach( m => {
        log.info(m)
        val mapConfig = new MapConfig()
        mapConfig.setName(m)
        mapConfig.setBackupCount(hazelcastConfig.hosts.length)
        mapConfig.setStatisticsEnabled(true)
        mapConfig.getMaxSizeConfig.setSize(10000)
        config.addMapConfig(mapConfig)
      })
      hazelcastInstance = Hazelcast.newHazelcastInstance(config)
    }
  }

  def map[K, V](name: String) = {
    if(hazelcastInstance == null) throw new IllegalStateException("Hazelcast not initialized")
    val cacheMap: IMap[K, V] = hazelcastInstance.getMap[K, V](name)
    cacheMap
  }
}
