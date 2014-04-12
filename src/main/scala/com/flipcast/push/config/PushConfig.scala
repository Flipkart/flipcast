package com.flipcast.push.config

/**
 * PushConfig
 *
 * @author Phaneesh Nagaraja
 */
case class PushConfig(configName: String, gcm: GcmConfig, apns: ApnsConfig, mpns: MpnsConfig)
