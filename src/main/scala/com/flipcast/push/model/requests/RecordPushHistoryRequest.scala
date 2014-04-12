package com.flipcast.push.model.requests

/**
 * Model class to represent a push history event
 */
case class RecordPushHistoryRequest(configName: String, key: String, message: String)
