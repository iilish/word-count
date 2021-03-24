package org.irach.challenge.module.state

import org.irach.challenge.domain.{RawEvent, _}
import org.irach.challenge.module.LoggingMock.Info
import zio.logging.{LogContext, Logger}
import zio.test.Assertion.equalTo
import zio.test._
import zio.{Ref, UIO, ZIO, ZLayer}

object InternalStateSpec extends DefaultRunnableSpec {
  private val mockLogging = ZLayer.succeed {
    new Logger[String] {
      override def locally[R1, E, A1](f: LogContext => LogContext)(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1] = ???

      override def log(line: => String): UIO[Unit] = ???

      override def logContext: UIO[LogContext] = ???

      override def debug(line: => String): UIO[Unit] = ZIO.succeed()

      override def warn(line: => String): UIO[Unit] = ZIO.succeed()

      override def info(line: => String): UIO[Unit] = ZIO.succeed(())
    }
  }

  private val tumbling = TimeTumbling().asInstanceOf[Strategy]
  private val rawEvent = RawEvent("", "", 1)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("InternalState")(
    testM("setStrategy delegate to ref.update") {
      // Given
      val ref = Ref.make(tumbling)
      val env = ref.toLayer ++ Info(equalTo(s"Setting strategy $tumbling")) >>> InternalState.live

      // When
      val result = InternalState.setStrategy(tumbling).provideLayer(env)

      // Then
      assertM(result)(equalTo(tumbling))
    },

    testM("getStrategy delegate to ref.get") {
      // Given
      val ref = Ref.make(tumbling)
      val env = ref.toLayer ++ Info(equalTo(s"current strategy $tumbling")) >>> InternalState.live

      // When
      val result = InternalState.getStrategy.provideLayer(env)

      // Then
      assertM(result)(equalTo(tumbling))
    },

    testM("IsWindow verifies for EventCount current == limit => true (Window end)") {
      // Given
      val ref = Ref.make(EventCount(10, 10).asInstanceOf[Strategy])
      val env = ref.toLayer ++ Info(equalTo("Window end reached")) >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(BadRawEvent("", "").asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(true))
    },

    testM("IsWindow verifies for EventCount limit < current => true (Window end)") {
      // Given
      val ref = Ref.make(EventCount(5, 10).asInstanceOf[Strategy])
      val env = ref.toLayer ++ Info(equalTo("Window end reached")) >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(rawEvent.asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(true))
    },

    testM("IsWindow verifies for EventCount limit > current => false (not Window end)") {
      // Given
      val ref = Ref.make(EventCount(15, 10).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(Watermark(1).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(false))
    },

    testM("IsWindow verifies for TimeTumbling if event is Watermark => true (Window end)") {
      // Given
      val ref = Ref.make(TimeTumbling().asInstanceOf[Strategy])
      val env = ref.toLayer ++ Info(equalTo("Window end reached")) >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(Watermark(1).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(true))
    },

    testM("isWindow verifies for TimeTumbling if event is not Watermark => false (not Window end)") {
      // Given
      val ref = Ref.make(TimeTumbling().asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(BadRawEvent("", "").asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(false))
    },

    testM("isWindow verifies for EventTime limit == (end - begin) => true (Window end)") {
      // Given
      val ref = Ref.make(EventTime(5, 10, 15).asInstanceOf[Strategy])
      val env = ref.toLayer ++ Info(equalTo("Window end reached")) >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(Watermark(1).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(true))
    },

    testM("isWindow verifies for EventTime limit < (end - begin) => true (Window end)") {
      // Given
      val ref = Ref.make(EventTime(5, 10, 16).asInstanceOf[Strategy])
      val env = ref.toLayer ++ Info(equalTo("Window end reached")) >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(RawEvent("", "", 1).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(true))
    },

    testM("isWindow verifies for EventCount limit > (end - begin) => false (not Window end)") {
      // Given
      val ref = Ref.make(EventTime(5, 10, 14).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.isWindowEnd(Watermark(1).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(false))
    },

    testM("eventReceived for STRATEGY=EventCount and EVENT=AppRawEvent the current count is incremented") {
      // Given
      val ref = Ref.make(EventCount(25, 10).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.eventReceived(AppRawEvent(1, rawEvent).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(EventCount(25, 11).asInstanceOf[Strategy]))
    },

    testM("eventReceived for STRATEGY=EventCount and EVENT!=AppRawEvent the current count is NOT incremented") {
      // Given
      val ref = Ref.make(EventCount(25, 10).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.eventReceived(BadRawEvent("","").asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(EventCount(25, 10).asInstanceOf[Strategy]))
    },

    testM("eventReceived for STRATEGY=EventTime and EVENT=AppRawEvent the end time count is updated with event.timestamp") {
      // Given
      val ref = Ref.make(EventTime(25, 10, 30).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.eventReceived(AppRawEvent(1, RawEvent("", "", 35)).asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(EventTime(25, 10, 35).asInstanceOf[Strategy]))
    },

    testM("eventReceived for STRATEGY=EventTime and EVENT!=AppRawEvent the end time count is NOT updated with event.timestamp") {
      // Given
      val ref = Ref.make(EventTime(25, 10, 30).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.eventReceived(BadRawEvent("", "").asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(EventTime(25, 10, 30).asInstanceOf[Strategy]))
    },
    testM("eventReceived for STRATEGY=TimeTumbling the same strategy is returned") {
      // Given
      val ref = Ref.make(TimeTumbling().asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.eventReceived(BadRawEvent("", "").asInstanceOf[ProcessEvent]).provideLayer(env)

      // Then
      assertM(result)(equalTo(TimeTumbling().asInstanceOf[Strategy]))
    },

    testM("initialize for STRATEGY=EventCount the count is reset") {
      // Given
      val ref = Ref.make(EventCount(100, 51).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.initialize.provideLayer(env)

      // Then
      assertM(result)(equalTo(EventCount(100).asInstanceOf[Strategy]))
    },

    testM("initialize for STRATEGY=EventTime => new.beginTime and new.endTime are set to old.endTime") {
      // Given
      val ref = Ref.make(EventTime(100, 50, 150).asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.initialize.provideLayer(env)

      // Then
      assertM(result)(equalTo(EventTime(100, 150, 150).asInstanceOf[Strategy]))
    },

    testM("initialize for STRATEGY=TimeTumbling the same strategy is returned") {
      // Given
      val ref = Ref.make(TimeTumbling().asInstanceOf[Strategy])
      val env = ref.toLayer ++ mockLogging >>> InternalState.live

      // When
      val result = InternalState.initialize.provideLayer(env)

      // Then
      assertM(result)(equalTo(TimeTumbling().asInstanceOf[Strategy]))
    }
  )
}
