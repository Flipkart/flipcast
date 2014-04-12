package com.flipcast.model.responses

/**
 * Model class for status check response
 *
 * @author Phaneesh Nagaraja
 */
case class StatusCheckResponse(result: Boolean = true) extends ServiceResponse
