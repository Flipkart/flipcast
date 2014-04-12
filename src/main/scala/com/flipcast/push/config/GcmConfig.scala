package com.flipcast.push.config

/**
 * Config class to represent GCM configuration
 * @param apiKey GCM client api key
 * @param defaultDelayWhileIdle set the defaultDelayWhileIdle flag
 * @param defaultExpiry set default expiry
 *
 * @author Phaneesh Nagaraja
 */
case class GcmConfig(apiKey: String, defaultDelayWhileIdle: Boolean, defaultExpiry: Int = 300)
