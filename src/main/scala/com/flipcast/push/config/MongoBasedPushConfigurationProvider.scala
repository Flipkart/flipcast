package com.flipcast.push.config

import com.typesafe.config.Config
import com.flipcast.mongo.ConnectionHelper
import com.mongodb.casbah.Imports._
import collection.JavaConverters._
import com.mongodb.casbah.commons.MongoDBObject
import com.flipcast.Flipcast

class MongoBasedPushConfigurationProvider() (implicit config: Config) extends PushConfigurationProvider {

  lazy val configCollection = ConnectionHelper.collection(config.getString("source"))

  /**
   * Save/Update push configuration
   * @param pushConfig Push configuration
   */
  def save(pushConfig: PushConfig) = {
    val obj = MongoDBObject.newBuilder
    obj += "_id" -> pushConfig.configName
    obj += "gcm" -> MongoDBObject("apiKey" -> pushConfig.gcm.apiKey,
      "defaultDelayWhileIdle" -> pushConfig.gcm.defaultDelayWhileIdle,
      "defaultExpiry" -> pushConfig.gcm.defaultExpiry)
    obj += "apns" -> MongoDBObject("certificate" -> pushConfig.apns.certificate,
      "pass" -> pushConfig.apns.password, "sandbox" -> pushConfig.apns.sandbox,
      "defaultExpiry" -> pushConfig.apns.defaultExpiry)
    exists(pushConfig.configName) match {
      case true =>
        obj += "updated" -> System.currentTimeMillis()
      case false =>
        obj += "created" -> System.currentTimeMillis()
        obj += "updated" -> System.currentTimeMillis()
    }
    obj += "deleted" -> false
    val result = configCollection.update(MongoDBObject("_id" -> pushConfig.configName), obj.result(), true, false)
    result.getN > 0
  }

  /**
   * Delete a push config
   * @param configName Name of the configuration that needs to be deleted
   * @return operation result
   */
  def delete(configName: String) : Boolean = {
    val result = configCollection.remove(MongoDBObject("_id" -> configName))
    result.getN > 0
  }

  /**
   * Get a APNS configuration for a given configuration
   * @param configName Name of the configuration
   * @return The APNS Configuration for the given config name
   */
  private def apnsConfig(configName: String) = {
    val c = configCollection.findOne(MongoDBObject("_id" -> configName))
    transformToApnsConfig(c.getAsOrElse[DBObject]("apns", DBObject.empty))
  }

  /**
   * Internal helper method to convert mongo object to gcm config instance
   * @param obj MongoDBObject
   * @return GcmConfig
   */
  private def transformToApnsConfig(obj: MongoDBObject) = {
    ApnsConfig(obj.getAsOrElse[String]("certificate", "Unknown"),
      obj.getAsOrElse[String]("pass", "Unknown"),
      obj.getAsOrElse[Boolean]("sandbox", false),
      obj.getAsOrElse[Int]("defaultExpiry", 300))
  }

  /**
   * Get a GCM configuration for a given configuration
   * @param configName Name of the configuration
   * @return The GCM Configuration for the given config name
   */
  private def gcmConfig(configName: String) = {
    val c = configCollection.findOne(MongoDBObject("_id" -> configName))
    transformToGcmConfig(c.getAsOrElse[DBObject]("gcm", DBObject.empty))
  }

  /**
   * Internal helper method to convert mongo object to gcm config instance
   * @param obj MongoDBObject
   * @return GcmConfig
   */
  private def transformToGcmConfig(obj: MongoDBObject) = {
    GcmConfig(obj.getAsOrElse[String]("apiKey", "Unknown"),
      obj.getAsOrElse[Boolean]("defaultDelayWhileIdle", false),
      obj.getAsOrElse[Int]("defaultExpiry", 300))
  }

  /**
   * Get a MPNS configuration for a given configuration
   * @param configName Name of the configuration
   * @return The MPNS Configuration for the given config name
   */
  private def mpnsConfig(configName: String) = {
    val c = configCollection.findOne(MongoDBObject("_id" -> configName))
    transformToMpnsConfig(c)
  }


  /**
   * Internal helper method to convert mongo object to mpns config instance
   * @param obj MongoDBObject
   * @return MpnsConfig
   */
  private def transformToMpnsConfig(obj: MongoDBObject) = {
    MpnsConfig()
  }

  /**
   * Reload the configuration.
   * This will make changing configuration easier
   */
  override def reload() = load()

  /**
   * Load the configuration set.
   * This will be called at bootstrap
   */
  override def load() = {
    ConnectionHelper.createCollection(config.getString("source")) (Flipcast.mongoConfig)
    try {
      val configs = configCollection.find()
      configs.iterator().asScala.map( c => {
        PushConfig(c.getAsOrElse[String]("_id", "Unknown"),
          transformToGcmConfig(c.getAsOrElse[DBObject]("gcm", DBObject.empty)),
          transformToApnsConfig(c.getAsOrElse[DBObject]("apns", DBObject.empty)),
          transformToMpnsConfig(c.getAsOrElse[DBObject]("mpns", DBObject.empty)))
      }).toList
    } catch {
      case ex: Exception =>
        log.error("Error loading push configuration", ex)
        List.empty
    }
  }

  /**
   * Get configuration for a given configuration
   * @param configName Name of the configuration
   * @return The push Configuration for the given config name
   */
  def config(configName: String) : PushConfig = {
    PushConfig(configName, gcmConfig(configName), apnsConfig(configName), mpnsConfig(configName))
  }

  /**
   * Get all configuration names
   * @return List fo all configuration names
   */
  def configs() : List[String] = {
    val configs = configCollection.find()
    configs.iterator().asScala.map( c => {
      c.getAsOrElse[String]("_id", "Unknown")
    }).toList
  }

  private def exists(config: String) = {
    configCollection.count(MongoDBObject("_id" -> config)) > 0
  }
}
