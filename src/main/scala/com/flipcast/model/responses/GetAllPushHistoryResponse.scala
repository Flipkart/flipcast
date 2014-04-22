package com.flipcast.model.responses

import com.flipcast.push.model.PushHistoryData

/**
 * Model class to represent get all push history response
 *
 * @author Phaneesh Nagaraja
 */
case class GetAllPushHistoryResponse(configName: String, data: PushHistoryData)
