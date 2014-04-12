package com.flipcast.push.gcm.service

import spray.can.Http
import spray.http.HttpHeaders.RawHeader

/**
 * GCM Service
 *
 * @author Phaneesh Nagaraja
 */
case class GCMService(client: Http.Connect, headers: RawHeader)
