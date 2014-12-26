package com.flipcast.push.model

import java.util.Date

/**
 * Model to represent sideline message
 */
case class SidelinedMessage(id: String, config: String, messageType: String, message: String, sentDate: Date)
