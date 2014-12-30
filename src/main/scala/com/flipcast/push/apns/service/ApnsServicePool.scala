package com.flipcast.push.apns.service

import akka.event.slf4j.Logger
import com.flipcast.push.config.PushConfigurationManager
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.notnoop.apns.{APNS, ApnsService}

/**
 * Service pool for APNS service
 */
object ApnsServicePool {

  val log = Logger("ApnsServicePool")

  private lazy val serviceCache: LoadingCache[String, ApnsService] = CacheBuilder.newBuilder()
    .maximumSize(5000)
    .concurrencyLevel(8)
    .recordStats()
    .build(
      new CacheLoader[String, ApnsService]() {
        def load(key: String) = {
          val config = PushConfigurationManager.config(key)
          val srv = config match {
            case Some(s) =>
              s.apns.sandbox match {
                case true =>
                  log.info("Creating apns service for config: " +s.apns)
                  APNS.newService()
                  .withCert(s.apns.certificate, s.apns.password)
                  .asQueued()
                  .withSandboxDestination()
                  .build()
                case false =>
                  log.info("Creating apns service for config: " +s.apns)
                  APNS.newService()
                    .withCert(s.apns.certificate, s.apns.password)
                    .asQueued()
                    .withProductionDestination()
                    .build()
              }
            case _ => null
          }
          if(srv != null) {
            srv.start()
          }
          srv
        }
      })

  def service(configName: String) = {
    try {
      val ser = serviceCache.get(configName)
      if(ser != null)
        ser.testConnection()
    } catch {
      case ex: Exception =>
        serviceCache.invalidate(configName)
    }
    serviceCache.get(configName)
  }

}
