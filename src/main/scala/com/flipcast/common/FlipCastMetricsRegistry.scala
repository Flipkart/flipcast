package com.flipcast.common

import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, ThreadStatesGaugeSet, MemoryUsageGaugeSet}

/**
 * Registry for all metrics elements
 *
 * @author Phaneesh Nagaraja
 */
object FlipCastMetricsRegistry {

  val metrics = new com.codahale.metrics.MetricRegistry()

  def registerDefaults() {
    metrics.registerAll(new MemoryUsageGaugeSet())
    metrics.registerAll(new ThreadStatesGaugeSet())
    metrics.registerAll(new GarbageCollectorMetricSet())
  }
}
