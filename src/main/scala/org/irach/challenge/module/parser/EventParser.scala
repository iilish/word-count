package org.irach.challenge.module.parser

import org.irach.challenge.domain._
import zio.macros.accessible
import zio.{Has, UIO, ZIO, ZLayer}

@accessible
object EventParser {

  import io.circe.generic.auto._
  import io.circe.parser._

  type EventParser = Has[EventParser.Service]

  trait Service {
    def decodeLine(appId: Int, blockString: String): UIO[ApplicationEvent]
  }

  lazy val live: ZLayer[Any, AppError, EventParser] =
    ZIO.succeed(new Service {
      override def decodeLine(appId: Int, json: String): UIO[ApplicationEvent] = ZIO.succeed(
        decode[RawEvent](json).fold(
          error => AppBadRawEvent(appId, BadRawEvent(json, error.getMessage)),
          decodedRaw => AppRawEvent(appId, decodedRaw)
        ))
    }).toLayer
}
