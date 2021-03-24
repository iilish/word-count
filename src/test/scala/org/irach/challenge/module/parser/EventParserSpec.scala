package org.irach.challenge.module.parser

import org.irach.challenge.domain.{AppBadRawEvent, AppRawEvent, BadRawEvent, RawEvent}
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

object EventParserSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("EventParser")(
    suite("to decode a line")(
      testM("decodeLine return BadRawEvent for an invalid json") {
        // GIVEN
        val block = "i-json"

        // WHEN
        val result = EventParser.decodeLine(1, block).provideLayer(EventParser.live)

        // THEN
        assertM(result)(equalTo(AppBadRawEvent(1, BadRawEvent("i-json", "expected json value got 'i-json' (line 1, column 1)"))))
      },

      testM("decodeLine return AppRawEvent for a valid json") {
        // GIVEN
        val block = """ {"event_type":"foo","data":"toto","timestamp":1} """

        // WHEN
        val result = EventParser.decodeLine(1, block).provideLayer(EventParser.live)

        // THEN
        assertM(result)(equalTo(AppRawEvent(1, RawEvent("foo", "toto", 1))))
      }
    )
  )
}
