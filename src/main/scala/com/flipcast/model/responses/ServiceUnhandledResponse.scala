package com.flipcast.model.responses

/**
 * Model class for sending unhandled response from a service
 * Returning this from process method in a service will translate to
 * BadRequest (HTTP 400)
 *
 * @author Phaneesh Nagaraja
 */
case class ServiceUnhandledResponse() extends ServiceResponse
