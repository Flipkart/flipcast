package com.flipcast.push.config

import akka.actor.{ActorLogging, Actor}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe


@SerialVersionUID(1L)
sealed trait PushConfigChangeMessage

case class PushConfigUpdatedMessage(configName: String, config: PushConfig) extends PushConfigChangeMessage

case class PushConfigDeletedMessage(configName: String) extends PushConfigChangeMessage

/**
 * Listen to changes in push configuration and then refresh the cache
 *
 * @author Phaneesh Nagaraja
 */
class PushConfigChangeListener extends Actor with ActorLogging {

  val mediator = DistributedPubSubExtension(context.system).mediator

  override def preStart(): Unit = {
    log.info("Listening for push configuration updates...")
    mediator ! Subscribe ("pushConfig", self)
  }


  def receive = {
    case msg: PushConfigUpdatedMessage =>
      log.info("Updating push config: " +msg.configName)
      PushConfigurationManager.updateConfig(msg.config)
    case msg: PushConfigDeletedMessage =>
      log.info("Deleting push config: " +msg.configName)
      PushConfigurationManager.deleteConfig(msg.configName)
  }
}
