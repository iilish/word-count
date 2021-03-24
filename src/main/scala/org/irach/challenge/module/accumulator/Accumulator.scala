package org.irach.challenge.module.accumulator

import org.irach.challenge.domain.{AppBadRawEvent, AppRawEvent, ProcessEvent}
import org.irach.challenge.module.state.EventState.EventState
import org.irach.challenge.module.state.InternalState.InternalState
import org.irach.challenge.module.state.{EventState, InternalState}
import zio.logging.{Logger, Logging}
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

@accessible
object Accumulator {
  type Accumulator = Has[Accumulator.Service]

  trait Service {
    def aggregate(raw: ProcessEvent): Task[Unit]
  }

  lazy val live: ZLayer[InternalState with EventState with Logging, Nothing, Accumulator] =
    ZLayer.fromServices[InternalState.Service, EventState.Service, Logger[String], Accumulator.Service] {
      (internalState, eventState, log) =>
        (raw: ProcessEvent) => {
          for {
            _ <- internalState.eventReceived(raw)
            _ <- log.debug(s"Receiving $raw")

            _ <- ZIO.succeed(raw.asInstanceOf[AppRawEvent].raw.event_type)
              .flatMap(eventType => {
                eventState.increment(eventType)
              })
              .when(raw.isInstanceOf[AppRawEvent])

            _ <- log.warn(s"Bad message received => $raw")
              .when(raw.isInstanceOf[AppBadRawEvent])

            isWindowEnd <- internalState.isWindowEnd(raw)

            _ <- (eventState.persist() <* eventState.initialize <* internalState.initialize <*
              log.info("Window end reached [current count will be persisted]"))
              .when(isWindowEnd)
          } yield ()
        }
    }
}