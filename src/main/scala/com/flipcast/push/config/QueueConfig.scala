package com.flipcast.push.config

/**
 * Config to hold queue configuration
 *
 * @author Phaneesh Nagaraja
 */
case class QueueConfig(inputQueueName: String, inputExchange: String,
                        sidelineQueueName: String, sidelineExchange: String, qos: Int)
