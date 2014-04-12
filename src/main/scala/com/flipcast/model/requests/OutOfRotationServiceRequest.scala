package com.flipcast.model.requests

import spray.routing.RequestContext

/**
 * Model class for Out of rotation request
 * @param ctx Request context
 *
 * @author Phaneesh Nagaraja
 */
case class OutOfRotationServiceRequest() (implicit val ctx: RequestContext)
