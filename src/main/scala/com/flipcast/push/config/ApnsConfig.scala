package com.flipcast.push.config

/**
 * Config class to represent APNS configuration
 * @param certificate path to certificate
 * @param password password to the certificate
 * @param sandbox Use sandbox destination
 *
 * @author Phaneesh Nagaraja
 */
case class ApnsConfig(certificate: String, password: String, sandbox: Boolean, defaultExpiry: Int = 300)
