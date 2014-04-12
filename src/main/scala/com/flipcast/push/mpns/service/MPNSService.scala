package com.flipcast.push.mpns.service

import spray.can.Http
import spray.http.HttpHeaders.RawHeader

/**
 * Model to represent a MPNS service for a configuration
 */
case class MPNSService(client: Http.Connect, headers: List[RawHeader])
