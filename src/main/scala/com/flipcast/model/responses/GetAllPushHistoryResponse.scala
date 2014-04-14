package com.flipcast.model.responses

/**
 * Model class to represent get all push history response
 */
case class GetAllPushHistoryResponse(configName: String, total: Long, details: Map[String, Long])
