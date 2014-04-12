package com.flipcast.model.responses

/**
 * Model class for wrapping any service failure/errors
 *
 * @author Phaneesh Nagaraja
 */
case class ServiceFailureResponse(error: Throwable) extends ServiceResponse
