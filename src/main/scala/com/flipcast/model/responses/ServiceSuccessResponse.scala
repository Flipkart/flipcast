package com.flipcast.model.responses

/**
 * Model class for wrapping any successful service response
 *
 * @author Phaneesh Nagaraja
 */
case class ServiceSuccessResponse[T](data: T) extends ServiceResponse[T]
