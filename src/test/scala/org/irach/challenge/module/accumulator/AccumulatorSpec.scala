package org.irach.challenge.module.accumulator

import org.irach.challenge.domain._
import org.irach.challenge.module.mock.{EventStateMock, InternalStateMock}
import zio.test.Assertion.{containsString, equalTo, isRight}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.value

object AccumulatorSpec extends DefaultRunnableSpec {

  import Accumulator._
  import org.irach.challenge.module.LoggingMock._
  import org.irach.challenge.module.mock.EventStateMock._
  import org.irach.challenge.module.mock.InternalStateMock._

  private val timeTumblingAsValue= value(TimeTumbling().asInstanceOf[Strategy])
  override def spec: ZSpec[TestEnvironment, Any] = suite("Accumulator")(
    suite("to aggregate a processEvent")(
      testM("AppRawEvent delegates to State.increment") {
        // Given
        val event = AppRawEvent(1, RawEvent("eventType", "data", 1L))
        val env = (
          EventReceived(equalTo(event.asInstanceOf[ProcessEvent]), timeTumblingAsValue) &&
            Increment(equalTo("eventType"), value(1L)) &&
            Debug(equalTo(s"Receiving $event")) &&
            IsWindowEnd(equalTo(event.asInstanceOf[ProcessEvent]), value(false))
          ) >>> Accumulator.live

        // When
        val result = aggregate(event).provideLayer(env).either

        // Then
        assertM(result)(isRight(equalTo(())))
      }
      ,

      testM("Not treated event log a debug message") {
        // Given
        val event = BadRawEvent("", "")
        val env =
          (EventReceived(equalTo(event.asInstanceOf[ProcessEvent]), timeTumblingAsValue) &&
            (EventStateMock.Persist() || Debug(equalTo(s"Receiving $event"))) &&
            IsWindowEnd(equalTo(event.asInstanceOf[ProcessEvent]), value(false))
            ) >>> Accumulator.live

        // When
        val result = Accumulator.aggregate(event).provideLayer(env).either

        // Then
        assertM(result)(isRight(equalTo(())))
      },

      testM("AppBadRawEvent log a warning message") {
        // Given
        val event = AppBadRawEvent(1, BadRawEvent("", ""))
        val env = (
          EventReceived(equalTo(event.asInstanceOf[ProcessEvent]), timeTumblingAsValue) &&
            (EventStateMock.Persist() || Warn(equalTo(s"Bad message received => $event")) &&
              Debug(equalTo(s"Receiving $event"))) &&
            IsWindowEnd(equalTo(event.asInstanceOf[ProcessEvent]), value(false))
          ) >>>
          Accumulator.live

        // When
        val result = Accumulator.aggregate(event).provideLayer(env).either

        // Then
        assertM(result)(isRight(equalTo(())))
      },

      testM("Watermark persist and initialize state") {
        // Given
        val watermark = Watermark(1)
        val env = (
          EventReceived(equalTo(watermark.asInstanceOf[ProcessEvent]), timeTumblingAsValue) &&
            (EventStateMock.Persist() && EventStateMock.Initialize(value(Map[String, Long]())) &&
              InternalStateMock.Initialize(timeTumblingAsValue) && Info(containsString("[current count will be persisted]")) &&
              Debug(equalTo(s"Receiving $watermark"))) &&
            IsWindowEnd(equalTo(watermark.asInstanceOf[ProcessEvent]), value(true))
          ) >>> Accumulator.live

        // When
        val result = Accumulator.aggregate(watermark).provideLayer(env).either

        // Then
        assertM(result)(isRight(equalTo(())))
      }
    )
  )
}

