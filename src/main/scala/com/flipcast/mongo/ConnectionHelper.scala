package com.flipcast.mongo

import akka.event.slf4j.Logger
import com.flipcast.model.config.MongoConfig
import com.mongodb.casbah.{Imports, MongoClientOptions, MongoClient}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.flipcast.Flipcast


/**
 * MongoDB connection helper
 *
 * @author Phaneesh Nagaraja
 */
object ConnectionHelper {

  val log = Logger(this.getClass.getSimpleName)

  var dbClient: MongoClient = null

  var dbInstance: MongoDB = null

  def init() (implicit config: MongoConfig) {
    if(dbClient == null) {
      log.info("*****************************************************")
      log.info("Initializing datastore client")
      log.info("*****************************************************")
      log.info("Database: %s".format(config.database))
      log.info("Hosts:" +config.hosts.mkString(","))
      val options = MongoClientOptions (
        autoConnectRetry = true,
        socketKeepAlive = true,
        connectionsPerHost = config.connectionsPerHost,
        connectTimeout = config.connectTimeout,
        cursorFinalizerEnabled = true,
        maxAutoConnectRetryTime = config.maxAutoConnectRetryTime,
        maxWaitTime = config.maxWaitTime,
        socketTimeout = config.socketTimeout,
        writeConcern = WriteConcern.Safe,
        readPreference = ReadPreference.SecondaryPreferred,
        threadsAllowedToBlockForConnectionMultiplier = Runtime.getRuntime.availableProcessors() / 2
      )
      val servers = config.hosts.map( h => {
        val tokens = h.split(":")
        new Imports.ServerAddress(tokens(0), tokens(1).toInt)
      }).toList
      val credentials = config.user match {
        case Some(user) =>
          config.password match {
            case Some(pass) => MongoCredential(user, config.database, pass.toCharArray)
            case _ => MongoCredential(user, config.database, "".toCharArray)
          }
        case _ => None
      }
      val conn = credentials match {
        case c: MongoCredential => MongoClient(servers, List(c), options)
        case _ => MongoClient(servers, options)
      }
      dbClient = conn
      createDatabase()
      dbInstance = database()
    }
  }

  def connection = {
    dbClient
  }

  def database() (implicit config: MongoConfig) = {
    dbClient.getDB(config.database)
  }

  def collection(name: String) = {
    dbInstance.getCollection(name)
  }

  private def createDatabase() (implicit config: MongoConfig) {
    dbClient.dbNames.count( n => n == config.database) match {
      case 0 =>
        dbClient.getDB(config.database)
        log.info("Database created: " +config.database)
        config.sharding match {
          case true =>
            log.info("Sharding is enabled!")
            val adminDB = dbClient.getDB("admin")
            val result = adminDB.command(MongoDBObject("enableSharding" -> config.database))
            log.info("Enable database sharding command result: " +result)
          case _ => None
        }
      case _ =>
        log.warn("Database already exists!: " +config.database)
    }
  }

  def isCollectionPresent(name: String) (implicit config: MongoConfig) = {
    database().collectionExists(name)
  }


  def createCollection(name: String) (implicit config: MongoConfig) {
    isCollectionPresent(name) match {
      case true =>
        log.warn("Collection " +name +" already present")
      case false =>
        val collection = dbInstance.createCollection(name, MongoDBObject())
        Flipcast.mongoConfig.sharding match {
          case true =>
            val shardKey = MongoDBObject("_id" -> "hashed")
            collection.ensureIndex(shardKey)
            val result = dbClient.getDB("admin").command(MongoDBObject("shardCollection" -> name, "key" -> shardKey))
            log.info("Sharding sys_relations_metadata collection result: " +result)
          case _ => None
        }
    }
  }

  /**
   * Create index for a given document attribute/attributes in a collection
   * @param collectionName Name of the collection
   * @param keys Keys that needs to be indexed
   * @param config MongoConfig
   * @return
   */
  def createIndex(collectionName: String, keys: Seq[(String, Int)]) (implicit config: MongoConfig)  {
    val idxName = "idx_" +collectionName +"_" +keys.mkString("_")
    val collection = dbInstance.getCollection(collectionName)
    val indexKeys = MongoDBObject.newBuilder
    keys.foreach( k => indexKeys += k._1 -> k._2)
    collection.ensureIndex(indexKeys.result(), MongoDBObject("background" -> true, "name" -> idxName, "unique" -> false))
  }

}
