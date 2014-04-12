package com.flipcast.push.gcm.model

case class GcmResult(message_id: Option[String] = None,
                     registration_id: Option[String] = None,
                     error: Option[String] = None)