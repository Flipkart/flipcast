package com.flipcast.model.requests

import spray.routing.RequestContext

/**
 * Model class for In rotation request
 * @param ctx Request context
 *
 * @author Phaneesh Nagaraja
 */
case class InRotationServiceRequest() (implicit val ctx: RequestContext)
