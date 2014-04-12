package com.flipcast.model.config

import com.typesafe.config.Config
import collection.JavaConverters._

/**
 * MongoDB config
 *
 * @author Phaneesh Nagaraja
 */
case class MongoConfig(config: Config) {

  def hosts : List[String] = {
    try {
      config.getStringList("hosts").asScala.toList
    } catch {
      case ex: Exception => List.empty[String]
    }
  }

  def database : String = {
    config.getString("database")
  }

  def user: Option[String] = {
    try {
      Option(config.getString("user"))
    } catch {
      case ex: Exception => None
    }
  }

  def password: Option[String] = {
    try {
      Option(config.getString("password"))
    } catch {
      case ex: Exception => None
    }
  }

  def connectTimeout: Int = {
    try {
      config.getInt("connectTimeout")
    } catch {
      case ex: Exception => 10000
    }
  }

  def socketTimeout: Int = {
    try {
      config.getInt("socketTimeout")
    } catch {
      case ex: Exception => 10000
    }
  }

  def connectionsPerHost : Int = {
    try {
      config.getInt("connectionsPerHost")
    } catch {
      case ex: Exception => 10
    }
  }

  def maxAutoConnectRetryTime : Int = {
    try {
      config.getInt("maxAutoConnectRetryTime")
    } catch {
      case ex: Exception => 100
    }
  }

  def maxWaitTime : Int = {
    try {
      config.getInt("maxWaitTime")
    } catch {
      case ex: Exception => 1000
    }
  }

  def sharding : Boolean = {
    try {
      config.getBoolean("sharding")
    } catch {
      case ex: Exception => false
    }
  }

}
