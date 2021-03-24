package org.irach.challenge.module.api

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.irach.challenge.module.repo.Repository
import org.irach.challenge.module.repo.Repository.{Repository, readAll}
import zio.interop.catz._
import zio.{RIO, ZIO}

class EventCountEndPoint[R <: Repository] {
  type EventCountTask[A] = RIO[R, A]

  private val prefixPath = "/events/count"

  val dsl: Http4sDsl[EventCountTask] = Http4sDsl[EventCountTask]

  import dsl._
  import io.circe.syntax._

  private val httpRoutes = HttpRoutes.of[EventCountTask] {
    case GET -> Root / eventType =>
      for {
        optionEventType <- Repository.read(eventType)
        eventType <- ZIO.succeed(optionEventType.fold("{}")(_.asJson.noSpaces))
        json <- Ok(eventType)
      } yield json

    case GET -> Root => for {
      events <- readAll()
      jsonEvents <- ZIO.succeed(events.map(_.asJson.noSpaces).mkString("[",",","]"))
      json <- Ok.apply(jsonEvents)
    } yield json
  }

  val routes: HttpRoutes[EventCountTask] = Router(prefixPath -> httpRoutes)
}
