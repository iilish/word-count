package org.irach.challenge.module.state

import org.irach.challenge.domain._
import zio._
import zio.logging.{Logger, Logging}
import zio.macros.accessible

@accessible
object InternalState {
  type InternalState = Has[Service]

  trait Service {
    def setStrategy(strategy: Strategy): Task[Strategy]

    def getStrategy: Task[Strategy]

    def eventReceived(event: ProcessEvent): Task[Strategy]

    def isWindowEnd(event: ProcessEvent): Task[Boolean]

    def initialize: Task[Strategy]
  }

  lazy val live: ZLayer[Has[Ref[Strategy]] with Logging, Nothing, InternalState] =
    ZLayer.fromServices[Ref[Strategy], Logger[String], InternalState.Service](
      (ref, log) => new Service {
        override def setStrategy(strategy: Strategy): Task[Strategy] = for {
          _ <- log.info(s"Setting strategy $strategy")
          _ <- ref.update(_ => strategy)
        } yield strategy

        override def getStrategy: Task[Strategy] = for {
          strategy <- ref.get
          _ <- log.info(s"current strategy $strategy")
        } yield strategy

        override def isWindowEnd(event: ProcessEvent): Task[Boolean] = for {
          strategy <- ref.get
          isWindowEnd <- ZIO.succeed {
            strategy match {
              case EventCount(limit, current) =>
                limit <= current
              case TimeTumbling() =>
                event.isInstanceOf[Watermark]
              case EventTime(limit, begin, current) =>
                limit <= current - begin
            }
          }
          _ <- log.info(s"Window end reached").when(isWindowEnd)
        } yield isWindowEnd

        override def eventReceived(event: ProcessEvent): Task[Strategy] = for {
          currentStrategy <- ref.get
          _ <- ref.update {
            case EventCount(limit, current) if event.isInstanceOf[AppRawEvent] =>
              EventCount(limit, current + 1)
            case EventTime(limit, beginTime, _) if event.isInstanceOf[AppRawEvent] =>
              val currentEventTime = event.asInstanceOf[AppRawEvent].raw.timestamp
              EventTime(limit, beginTime, currentEventTime)
            case currentStrategy => currentStrategy
          }
          newStrategy <- ref.get
          _ <- log.info(s" before $currentStrategy after $newStrategy")
            .when(currentStrategy.isInstanceOf[EventCount] || currentStrategy.isInstanceOf[EventTime])

        } yield newStrategy

        override def initialize: Task[Strategy] = for {
          _ <- ref.update {
            case EventCount(limit, _) =>
              EventCount(limit)
            case EventTime(limit, _, currentTime) =>
              EventTime(limit, currentTime, currentTime)
            case currentStrategy => currentStrategy
          }
          strategy <- ref.get
        } yield strategy
      }
    )
}