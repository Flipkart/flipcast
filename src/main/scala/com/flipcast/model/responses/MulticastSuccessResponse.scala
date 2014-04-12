package com.flipcast.model.responses

/**
 * Model to represent Multicast push success response
 */
case class MulticastSuccessResponse(total: Long, batches: Long) extends ServiceResponse[MulticastSuccessResponse]
