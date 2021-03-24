package org.irach.challenge

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

package object domain {

  sealed trait ProcessEvent

  case class Watermark(time: Long) extends ProcessEvent


  sealed trait ApplicationEvent extends ProcessEvent

  case class RawEvent(event_type: String, data: String, timestamp: Long) extends ApplicationEvent

  case class BadRawEvent(raw: String, errorMsg: String) extends ApplicationEvent

  case class AppRawEvent(appId: Int, raw: RawEvent) extends ApplicationEvent

  case class AppBadRawEvent(appId: Int, raw: BadRawEvent) extends ApplicationEvent


  sealed trait Strategy

  case class EventCount(limit: Int, current : Int = 0) extends Strategy

  case class TimeTumbling() extends Strategy

  case class EventTime(limit: Long, beginTime: Long, endTime : Long) extends Strategy


  sealed trait AppError

  case class ParseError(raw: String) extends AppError

  final case class Event(eventType: String, count: Long)

  object Event {
    implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  }

}