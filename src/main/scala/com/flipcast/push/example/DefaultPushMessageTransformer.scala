package com.flipcast.push.example

import com.flipcast.push.model.{DeviceOperatingSystemType, PushMessageData}
import spray.json._
import akka.event.slf4j.Logger
import com.flipcast.push.common.PushMessageTransformer

/**
 * A simple message transformer
 *
 * @author Phaneesh Nagaraja
 */
object DefaultPushMessageTransformer extends PushMessageTransformer with DefaultPushMessageProtocolSupport {

  val log = Logger("DefaultPushMessageTransformer")

  override def transform(configName: String, message: String) = {
    val msg = JsonParser(message).convertTo[DefaultPushMessage]
    val data = new PushMessageData()
    data.addPayload(DeviceOperatingSystemType.ANDROID, gcmMessage(msg))
    data.addPayload(DeviceOperatingSystemType.iOS, apnsMessage(msg))
    data
  }

  def gcmMessage(message: DefaultPushMessage) = {
    DefaultGcmMessage(message.title, message.message).toJson.compactPrint
  }

  def apnsMessage(message: DefaultPushMessage) = {
    DefaultApnsMessage(message.title +" - " +message.message).toJson.compactPrint
  }

}
