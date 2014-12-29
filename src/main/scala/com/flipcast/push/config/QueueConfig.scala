package com.flipcast.push.config

/**
 * Config to hold queue configuration
 *
 * @author Phaneesh Nagaraja
 */
case class QueueConfig(workerName: String, sidelineWorkerName: String, workerInstances: Int = 1)
