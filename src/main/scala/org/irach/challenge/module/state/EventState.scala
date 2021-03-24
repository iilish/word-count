package org.irach.challenge.module.state

import org.irach.challenge.module.repo.Repository
import org.irach.challenge.module.repo.Repository.Repository
import zio.logging.{Logger, Logging}
import zio.macros.accessible
import zio.{Has, Ref, Task, ZLayer}

@accessible
object EventState {
  type EventState = Has[EventState.Service]

  trait Service {

    def increment(eventType: String): Task[Long]

    def initialize: Task[Map[String, Long]]

    def persist(): Task[Unit]

    def load(): Task[Map[String, Long]]
  }

  lazy val live: ZLayer[Has[Ref[Map[String, Long]]] with Repository with Logging, Nothing, EventState] = {
    ZLayer
      .fromServices[Ref[Map[String, Long]], Repository.Service, Logger[String], EventState.Service](
        (cache, eventRepo, log) =>
          new Service {
            override def increment(eventType: String): Task[Long] = for {
              _ <- cache.update(cache => {
                val currentCountPlusOne = cache.get(eventType).fold(1L)(v => v + 1)
                cache + (eventType -> currentCountPlusOne)
              })
              value <- cache.get.map(c => c(eventType))
              _ <- log.info(s"current state of $eventType => $value")
            } yield value

            override def initialize: Task[Map[String, Long]] = for {
              _ <- log.debug("initializing current state")
              _ <- cache.update(_ => Map())
              value <- cache.get
            } yield value

            override def persist(): Task[Unit] = for {
              events <- cache.get
              _ <- log.debug(s"Saving $events into DB")
              _ <- eventRepo.writeAll(events)
            } yield ()

            override def load(): Task[Map[String, Long]] = for {
              events <- eventRepo.readAll()
              _ <- cache.update(_ => events.map(event => (event.eventType, event.count)).toMap)
              state <- cache.get
            } yield state
          }
      )
  }
}
