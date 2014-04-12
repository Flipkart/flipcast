package com.flipcast.common

import akka.event.slf4j.Logger
import spray.routing.RequestContext
import spray.http.HttpResponse
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Helper to perform access logging
 *
 * @author Phaneesh Nagaraja
 */
object AccessLogger {

  private val log = Logger("access_log")

  private val dateFormatter = new SimpleDateFormat("dd/MMM/yyyy:kk:mm:ss Z")

  def access(ctx: RequestContext, response: HttpResponse) {
    log.info("""%s - [%s] "%s %s %s" %s %s "%s" "%s"""".format(
      remoteAddress(ctx),
      dateFormatter.format(new Date(System.currentTimeMillis())),
      ctx.request.method.value,
      ctx.request.uri.path,
      ctx.request.protocol.value,
      response.status.value.toString,
      response.entity.data.length,
      referer(ctx),
      userAgent(ctx)))
  }

  private def referer(ctx: RequestContext) = {
    val refererHeader = ctx.request.headers.filter( h => h.lowercaseName == "referer")
    refererHeader.size match {
      case 0 => "Unknown"
      case _ => refererHeader.head.value
    }

  }

  private def remoteAddress(ctx: RequestContext) = {
    val address = ctx.request.headers.count( h => h.lowercaseName == "remote-address") match {
      case 0 => "Unknown"
      case _ => ctx.request.headers.filter( h => h.lowercaseName == "remote-address").head.value
    }
    address
  }

  private def userAgent(ctx: RequestContext) = {
    val userAgent = ctx.request.headers.filter( h => h.lowercaseName == "user-agent")
    val agentString = userAgent.length match {
      case 0 => "Unknown"
      case _ => userAgent.map( _.value).mkString(",")
    }
    agentString
  }

}
