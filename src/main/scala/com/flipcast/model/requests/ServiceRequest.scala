package com.flipcast.model.requests

import spray.routing.RequestContext

/**
 * Model class for wrapping any service request
 *
 * @author Phaneesh Nagaraja
 */
case class ServiceRequest[T](data: T)(implicit val ctx: RequestContext)
