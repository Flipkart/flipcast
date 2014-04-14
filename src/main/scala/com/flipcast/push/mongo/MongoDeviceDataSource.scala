package com.flipcast.push.mongo

import com.flipcast.push.common.DeviceDataSource
import com.flipcast.push.config.PushConfigurationManager
import com.flipcast.mongo.ConnectionHelper
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.util.JSON
import com.mongodb.casbah.Imports._
import com.flipcast.push.model.{DeviceOperatingSystemType, DeviceData}
import collection.JavaConverters._
import com.flipcast.model.config.MongoConfig
import com.flipcast.Flipcast
import java.util.{Date, UUID}
import com.mongodb.DBCollection
import akka.event.slf4j.Logger

/**
 * MongoDB data source
 *
 * @author Phaneesh Nagaraja
 */
object MongoDeviceDataSource extends DeviceDataSource {

  val log = Logger("MongoDeviceDataSource")

  override def init() {
    implicit val config: MongoConfig = Flipcast.mongoConfig
    PushConfigurationManager.configs().foreach( c => {
      createDeviceDetailsCollection(c)
    })
    createPushHistoryCollection()
  }

  override def autoUpdateDeviceId(config: String, deviceIdentifier: String, newDeviceIdentifier: String) : Boolean = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val device = collection.findOne(buildQuery(Map("cloudMessagingId" -> deviceIdentifier)),
      MongoDBObject("_id" -> 1))
    if(device == null) {
      false
    } else {
      val result = collection.update(MongoDBObject("_id" -> device.getAsOrElse[String]("_id", "Unknown")),
        $set("cloudMessagingId" -> newDeviceIdentifier))
      result.getN > 0
    }
  }

  override def doHouseKeeping(config: String, deviceIdentifier: String) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val result = collection.remove(MongoDBObject("deviceId" -> deviceIdentifier))
    result.getN > 0
  }

  override def listAll(config: String, pageSize: Int, pageNo: Int) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val result = collection.find().skip(pageNo * pageSize).limit(pageSize)
    result.iterator().asScala.map( data => {
      DeviceData(config, data.getAsOrElse[String]("deviceId", "Unknown"),
        data.getAsOrElse[String]("cloudMessagingId", "Unknown"),
        DeviceOperatingSystemType.withName(data.getAsOrElse[String]("osName", "Unknown")),
        data.getAsOrElse[String]("osVersion", "Unknown"),
        data.getAsOrElse[String]("brand", "Unknown"),
        data.getAsOrElse[String]("model", "Unknown"),
        data.getAsOrElse[String]("appName", "Unknown"),
        data.getAsOrElse[String]("appVersion", "Unknown")
      )
    }).toList
  }

  override def list(config: String, filter: Map[String, Any], pageSize: Int, pageNo: Int) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val result = collection.find(buildQuery(filter, deleted = false, addDeleted = true)).skip(pageNo * pageSize).limit(pageSize)
    result.iterator().asScala.map( data => {
      DeviceData(config, data.getAsOrElse[String]("deviceId", "Unknown"),
        data.getAsOrElse[String]("cloudMessagingId", "Unknown"),
        DeviceOperatingSystemType.withName(data.getAsOrElse[String]("osName", "Unknown")),
        data.getAsOrElse[String]("osVersion", "Unknown"),
        data.getAsOrElse[String]("brand", "Unknown"),
        data.getAsOrElse[String]("model", "Unknown"),
        data.getAsOrElse[String]("appName", "Unknown"),
        data.getAsOrElse[String]("appVersion", "Unknown")
      )
    }).toList
  }

  override def get(config: String, filter: Map[String, Any]) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val result = collection.find(buildQuery(filter, deleted = false, addDeleted = true)).limit(1)
    val it = result.iterator()
    if(!it.hasNext)
      None
    else {
      val device = it.next()
      Option(DeviceData(config, device.getAsOrElse[String]("deviceId", "Unknown"),
        device.getAsOrElse[String]("cloudMessagingId", "Unknown"),
        DeviceOperatingSystemType.withName(device.getAsOrElse[String]("osName", "Unknown")),
        device.getAsOrElse[String]("osVersion", "Unknown"),
        device.getAsOrElse[String]("brand", "Unknown"),
        device.getAsOrElse[String]("model", "Unknown"),
        device.getAsOrElse[String]("appName", "Unknown"),
        device.getAsOrElse[String]("appVersion", "Unknown")
      ))
    }
  }

  override def count(config: String, filter: Map[String, Any]) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    collection.count(buildQuery(filter, deleted = false, addDeleted = true))
  }

  override def unregister(config: String, filter: Map[String, Any]) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val device = collection.findOne(buildQuery(filter), MongoDBObject("_id" -> 1))
    if(device == null) {
      false
    } else {
      val result = collection.update(MongoDBObject("_id" -> device.getAsOrElse[String]("_id", "Unknown")),
        $set("deleted" -> true))
      result.getN > 0
    }
  }

  override def register(config: String, deviceData: String,  filter: Map[String, Any]) = {
    val collection = ConnectionHelper.collection(config.toLowerCase +"_device_details")
    val data =  JSON.parse(deviceData).asInstanceOf[DBObject]
    upsertDevice(data, filter, collection)
    collection.update(MongoDBObject("_id" -> data.getAsOrElse[String]("_id", "Unknown")), data, true, false)
    DeviceData(config, data.getAsOrElse[String]("deviceId", "Unknown"),
      data.getAsOrElse[String]("cloudMessagingId", "Unknown"),
      DeviceOperatingSystemType.withName(data.getAsOrElse[String]("osName", "Unknown")),
      data.getAsOrElse[String]("osVersion", "Unknown"),
      data.getAsOrElse[String]("brand", "Unknown"),
      data.getAsOrElse[String]("model", "Unknown"),
      data.getAsOrElse[String]("appName", "Unknown"),
      data.getAsOrElse[String]("appVersion", "Unknown")
    )
  }

  override def recordHistory(config: String, key: String, message: String) = {
    val device = get(config, Map("cloudMessagingId" -> key) )
    device match {
      case Some(d) =>
        val collection = ConnectionHelper.collection("push_message_history")
        val data = MongoDBObject.newBuilder
        data += "_id" -> UUID.randomUUID().toString
        data += "deviceId" -> d.deviceId
        data += "appName" -> d.appName
        data += "appVersion" -> d.appVersion
        data += "brand" -> d.brand
        data += "cloudMessagingId" -> d.cloudMessagingId
        data += "configName" -> d.configName
        data += "model" -> d.model
        data += "osName" -> d.osName.toString
        data += "osVersion" -> d.osVersion
        data += "message" -> JSON.parse(message).asInstanceOf[DBObject]
        data += "sentDate" -> new Date()
        val result = collection.insert(data.result())
        result.getN > 0
      case _ =>
        log.warn("Invalid device: " +config +"[" +key +"]")
        false
    }
  }


  private def buildQuery(filter: Map[String, Any], deleted: Boolean = false, addDeleted: Boolean = false) = {
    val query = MongoDBObject.newBuilder
    filter.foreach( d => {
      query += d._1 -> d._2
    })
    addDeleted match {
      case false => query.result()
      case true => query.result() ++ ("deleted" -> deleted)
    }
  }

  private def upsertDevice(data: DBObject, filter: Map[String, Any], collection: DBCollection) {
    val device = collection.find(buildQuery(filter, deleted = true), MongoDBObject("_id" -> 1))
    val it = device.iterator()
    val id = it.hasNext match {
      case true =>
        data.put("updated", new Date())
        it.next().get("_id")
      case false =>
        data.put("updated", new Date())
        data.put("created", new Date())
        UUID.randomUUID().toString
    }
    data.contains("deleted") match {
      case true => None
      case false => data.put("deleted", false)
    }
    data.put("_id", id)
  }

  private def createDeviceDetailsCollection(configName: String) (implicit config: MongoConfig) {
    val collectionName = configName.toLowerCase +"_device_details"
    ConnectionHelper.createCollection(collectionName)
    ConnectionHelper.createIndex(collectionName, Seq(("deviceId", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("cloudMessagingId", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osVersion", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appVersion", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("updated", 0)))
    ConnectionHelper.createIndex(collectionName, Seq(("created", 0)))
    ConnectionHelper.createIndex(collectionName, Seq(("deleted", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1), ("updated", 0), ("deleted", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1), ("updated", 0), ("deleted", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1), ("osVersion", 1), ("updated", 0), ("deleted", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1), ("appVersion", 1), ("updated", 0), ("deleted", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appVersion", 1), ("updated", 0), ("deleted", 1)))
  }

  private def createPushHistoryCollection() (implicit config: MongoConfig) {
    val collectionName = "push_message_history"
    ConnectionHelper.createCollection(collectionName)
    ConnectionHelper.createIndex(collectionName, Seq(("deviceId", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("cloudMessagingId", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osVersion", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("model", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appVersion", 1)))
    ConnectionHelper.collection("push_message_history").ensureIndex(MongoDBObject("sentDate" -> 0, "expireAfterSeconds" -> 604800))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1), ("sentDate", 0), ("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1), ("sentDate", 0), ("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("osName", 1), ("osVersion", 1), ("sentDate", 0), ("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("appName", 1), ("appVersion", 1), ("sentDate", 0), ("brand", 1)))
    ConnectionHelper.createIndex(collectionName, Seq(("brand", 1), ("sentDate", 0)))
  }

}
