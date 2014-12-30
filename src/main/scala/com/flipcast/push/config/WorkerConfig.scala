package com.flipcast.push.config


case class PriorityConfig(workerInstances: Int = 1, workerName: String, sidelineWorkerName: String)

/**
 * Config to hold worker configuration
 *
 * @author Phaneesh Nagaraja
 */
case class WorkerConfig(configName: String, priorityConfigs: Map[String, PriorityConfig])
