package com.flipcast.push.example

import com.flipcast.push.model.{DeviceOperatingSystemType, PushMessageData}
import spray.json._
import akka.event.slf4j.Logger
import com.flipcast.push.common.PushMessageTransformer
import org.apache.commons.codec.binary.Base64

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
    data.addPayload(DeviceOperatingSystemType.WindowsPhone, mpnsMessage(msg))
    data
  }

  def gcmMessage(message: DefaultPushMessage) = {
    DefaultGcmMessage(message.title, message.message).toJson.compactPrint
  }

  def apnsMessage(message: DefaultPushMessage) = {
    DefaultApnsMessage(message.title +" - " +message.message).toJson.compactPrint
  }

  def mpnsMessage(message: DefaultPushMessage) = {
    val header = """<?xml version="1.0" encoding="utf-8"?>""" + """<wp:Notification xmlns:wp="WPNotification"><wp:Toast>"""
    val msg1 = """<wp:Text1>""" + message.title +"""</wp:Text1>"""
    val msg2 = """<wp:Text2>""" + message.message +"""</wp:Text2>"""
    val footer = """</wp:Toast></wp:Notification>"""
    Base64.encodeBase64String((header + msg1 + msg2 + footer).getBytes)
  }

}
