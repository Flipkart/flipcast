package com.flipcast.push.mpns.service

import java.io.FileInputStream
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import com.flipcast.Flipcast
import com.flipcast.push.config.MpnsConfig
import spray.can.Http
import spray.io.ClientSSLEngineProvider

/**
 * Key for service pool
 * @param configName Name of the configuration
 * @param host host name of subscription uri
 */
case class MpnsServiceKey(configName: String, host: String)

/**
 * A Simple connection pool to hold mpns connection
 * This will hold connection per config & host
 */
object MPNSServicePool {

  private val serviceCache = new ConcurrentHashMap[MpnsServiceKey, Http.Connect]()

  def service(configName: String, host: String) = {
    val key = MpnsServiceKey(configName, host)
    serviceCache.containsKey(key) match {
      case true =>
        serviceCache.get(key)
      case false =>
        val client = createClient(configName, host)
        serviceCache.put(key,client)
        serviceCache.get(key)
    }
  }

  private def createClient(configName: String, host: String) = {
    val config = Flipcast.pushConfigurationProvider.config(configName).mpns
    config.secured match {
      case false =>
        Http.Connect(host, sslEncryption = false)
      case true =>
        Http.Connect(host, sslEncryption = true) (ClientSSLEngineProvider { engine => createSSLEngine(config)})
    }
  }

  private def createSSLEngine(config: MpnsConfig) = {
    val ctx = sslContext(config)
    val engine = ctx.createSSLEngine()
    engine.setUseClientMode(true)
    engine
  }

  private def sslContext(config: MpnsConfig) = {
    val kms = keyManagers(config)
    val context = SSLContext.getInstance("TLS")
    context.init(kms, null, null)
    context
  }

  def keyManagers(config: MpnsConfig) = {
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    val ks = keyStore(config)
    keyManagerFactory.init(ks, config.pass.toCharArray)
    keyManagerFactory.getKeyManagers
  }

  private def keyStore(config: MpnsConfig) = {
    val keyStore = KeyStore.getInstance("PKCS12")
    val fis = new FileInputStream(config.certificate)
    keyStore.load(fis, config.pass.toCharArray)
    fis.close()
    keyStore
  }
}
