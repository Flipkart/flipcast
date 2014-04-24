package com.flipcast.push.config

/**
 * Config for MPNS service
 *
 * @author Phaneesh Nagaraja
 */
case class MpnsConfig(secured: Boolean = false, certificate: String = "None", pass: String = "None")
