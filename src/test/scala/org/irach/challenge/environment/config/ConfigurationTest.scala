package org.irach.challenge.environment.config

import org.irach.challenge.domain.{EventCount, EventTime, TimeTumbling}
import org.irach.challenge.environment.config.Configuration.SocketConfig
import org.scalatest.flatspec.AnyFlatSpec

class ConfigurationTest extends AnyFlatSpec {

  "buildStrategy" should "return TimeTumbling as default" ignore {
    // Given
    val socketConfig = SocketConfig("", 0, 0)

    // When
    val strategyBuild = socketConfig.buildStrategy

    // Then
    assert(strategyBuild === TimeTumbling())
  }

  it should "return TimeTumbling when a strategy is unknown" ignore {
    // Given
    val socketConfig = SocketConfig("", 0, 0, windowStrategy = "unknown-strategy")

    // When
    val strategyBuild = socketConfig.buildStrategy

    // Then
    assert(strategyBuild === TimeTumbling())
  }

  it should "return EventCount with limit set when `event-count` is defined" ignore {
    // Given
    val socketConfig = SocketConfig("", 0, 0, eventCountLimit = 100, windowStrategy = "event-count")

    // When
    val strategyBuild = socketConfig.buildStrategy

    // Then
    assert(strategyBuild === EventCount(100))
  }

  it should "return EventTime with limit set when `event-time` is defined" ignore {
    // Given
    val socketConfig = SocketConfig("", 0, 0, eventTimeLimit = 250, windowStrategy = "event-time")

    // When
    val strategyBuild = socketConfig.buildStrategy

    // Then
    assert(strategyBuild === EventTime(250, 0, 0))
  }
}
