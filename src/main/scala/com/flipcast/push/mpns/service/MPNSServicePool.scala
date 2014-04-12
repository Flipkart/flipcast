package com.flipcast.push.mpns.service

import com.google.common.cache.{CacheLoader, CacheBuilder}
import com.flipcast.Flipcast
import spray.can.Http
import spray.http.HttpHeaders

/**
 * Service pool for MPNS service
 */
object MPNSServicePool {

  private val serviceCache = CacheBuilder.newBuilder()
    .maximumSize(5000)
    .concurrencyLevel(30)
    .recordStats()
    .build(
      new CacheLoader[String, MPNSService]() {
        def load(key: String) = {
          val config = Flipcast.pushConfigurationProvider.config(key).gcm
          val gcmHttpClient = Http.Connect("android.googleapis.com", port=443, sslEncryption = true)
          val authHeader = HttpHeaders.RawHeader("Authorization", "key=" +config.apiKey)
          MPNSService(gcmHttpClient, List(authHeader))
        }
      })

  def service(configName: String) = {
    serviceCache.get(configName)
  }

}
