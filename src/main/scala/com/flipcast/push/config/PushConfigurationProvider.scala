package com.flipcast.push.config

import akka.event.slf4j.Logger


/**
 * PushConfigurationProvider for feeding in push configuration to push services
 *
 * @author Phaneesh Nagaraja
 */
trait PushConfigurationProvider {

  val log = Logger(this.getClass.getSimpleName)

  /**
   * Load the configuration set.
   * This will be called at bootstrap
   */
  def load() : List[PushConfig]

  /**
   * Reload the configuration.
   * This will make changing configuration easier
   */
  def reload() : List[PushConfig]

  /**
   * Get configuration for a given configuration
   * @param configName Name of the configuration
   * @return The push Configuration for the given config name
   */
  def config(configName: String) : PushConfig

  /**
   * Get all configuration names
   * @return List fo all configuration names
   */
  def configs() : List[String]

  /**
   * Save/Update push configuration
   * @param pushConfig Push configuration
   */
  def save(pushConfig: PushConfig) : Boolean

  /**
   * Delete a push config
   * @param configName Name of the configuration that needs to be deleted
   * @return operation result
   */
  def delete(configName: String) : Boolean

}
