package com.flipcast.push.common

import akka.event.slf4j.Logger
import com.flipcast.push.example.DefaultPushMessageTransformer
import java.util.concurrent.ConcurrentHashMap

/**
 * A registry for all message transformations per config (Multi config support)
 *
 * @author Phaneesh Nagaraja
 */
object PushMessageTransformerRegistry {

  val log = Logger("PushMessageTransformerRegistry")

  private val transformers = new ConcurrentHashMap[String, PushMessageTransformer]()

  def register(configName: String, transformer: PushMessageTransformer) {
    transformers.containsKey(configName) match {
      case true =>
        log.warn("Transformer already registered for: " +configName)
      case false =>
        log.info("Registering transformer for: " +configName)
        transformers.put(configName, transformer)
    }
  }

  def transformer(configName: String) : PushMessageTransformer = {
    transformers.containsKey(configName) match {
      case true => transformers.get(configName)
      case false =>
        log.warn("No transformers registered for config: " +configName +". Returning default transformer")
        DefaultPushMessageTransformer
    }
  }

}
