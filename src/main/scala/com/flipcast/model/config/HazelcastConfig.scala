package com.flipcast.model.config

import com.typesafe.config.Config
import collection.JavaConverters._

case class HazelcastConfig(config: Config) {

  def hosts : List[String] = {
    try {
      config.getStringList("hosts").asScala.toList
    } catch {
      case ex: Exception => List.empty[String]
    }
  }

  def port : Int = {
    try {
      config.getInt("post")
    } catch {
      case ex: Exception => 56000
    }
  }

  def password : String = {
    try {
      config.getString("password")
    } catch {
      case ex: Exception => "flipcast"
    }
  }


  def maps : List[String] = {
    try {
      config.getStringList("maps").asScala.toList
    } catch {
      case ex: Exception => List.empty[String]
    }
  }

}
