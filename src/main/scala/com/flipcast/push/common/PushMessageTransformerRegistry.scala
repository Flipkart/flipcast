package com.flipcast.push.common

import scala.collection.mutable
import akka.event.slf4j.Logger
import com.flipcast.push.example.DefaultPushMessageTransformer

/**
 * A registry for all message transformations per config (Multi config support)
 *
 * @author Phaneesh Nagaraja
 */
object PushMessageTransformerRegistry {

  val log = Logger("PushMessageTransformerRegistry")

  private val transformers = new mutable.HashMap[String, PushMessageTransformer]()
    with mutable.SynchronizedMap[String, PushMessageTransformer]

  def register(configName: String, transformer: PushMessageTransformer) {
    transformers.contains(configName) match {
      case true =>
        log.warn("Transformer already registered for: " +configName)
      case false =>
        log.info("Registering transformer for: " +configName)
        transformers += configName -> transformer
    }
  }

  def transformer(configName: String) : PushMessageTransformer = {
    transformers.contains(configName) match {
      case true => transformers(configName)
      case false =>
        log.warn("No transformers registered for config: " +configName +". Returning default transformer")
        DefaultPushMessageTransformer
    }
  }

}
