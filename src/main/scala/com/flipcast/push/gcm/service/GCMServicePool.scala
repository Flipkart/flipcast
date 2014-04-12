package com.flipcast.push.gcm.service

import com.google.common.cache.{CacheLoader, CacheBuilder}
import spray.can.Http
import spray.http.HttpHeaders
import com.flipcast.Flipcast

/**
 * Cached http service connection pool for GCM
 *
 * @author Phaneesh Nagaraja
 */
object GCMServicePool {

  private val serviceCache = CacheBuilder.newBuilder()
    .maximumSize(5000)
    .concurrencyLevel(30)
    .recordStats()
    .build(
      new CacheLoader[String, GCMService]() {
        def load(key: String) = {
          val config = Flipcast.pushConfigurationProvider.config(key).gcm
          val gcmHttpClient = Http.Connect("android.googleapis.com", port=443, sslEncryption = true)
          val authHeader = HttpHeaders.RawHeader("Authorization", "key=" +config.apiKey)
          GCMService(gcmHttpClient, authHeader)
        }
      })

  def service(configName: String) = {
    serviceCache.get(configName)
  }

}
