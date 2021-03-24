package org.irach.challenge.module.api

import cats.data.Kleisli
import cats.effect.ExitCode
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.AutoSlash
import org.http4s.{HttpRoutes, Request, Response}
import org.irach.challenge.module.repo.Repository.Repository
import zio.clock.Clock
import zio.interop.catz._
import zio._

object APIServer {
  type APIServer = Has[APIServer.Service]
  type LocalEnvironment = Clock with Repository
  type ServerRIO[A] = RIO[Clock with Repository, A]
  type ServerRoutes = Kleisli[ServerRIO, Request[ServerRIO], Response[ServerRIO]]

  trait Service {
    def start(host: String, port: Int): ZIO[Clock with Repository, Nothing, Unit]
  }

  lazy val live: ZLayer[Clock with Repository, Nothing, APIServer] = ZIO.succeed(new Service {
    val Root = "/"

    override def start(host: String, port: Int): ZIO[Clock with Repository, Nothing, Unit] = {

      ZIO.runtime[Clock with Repository].flatMap { implicit rts =>
        val ec = rts.platform.executor.asEC

        BlazeServerBuilder[ServerRIO](ec)
          .bindHttp(port, host)
          .withHttpApp(createRoutes(Root))
          .serve
          .compile[ServerRIO, ServerRIO, ExitCode]
          .drain
      }.orDie
    }

    def createRoutes(basePath: String): ServerRoutes = {
      val routes = new EventCountEndPoint[Clock with Repository].routes
      Router[ServerRIO](basePath -> middleware(routes)).orNotFound
    }

    private val middleware: HttpRoutes[ServerRIO] => HttpRoutes[ServerRIO] = {
      http: HttpRoutes[ServerRIO] => AutoSlash(http)
    }
  }
  ).toLayer

  def startAPIServer(host: String, port: Int): URIO[APIServer with Clock with Repository, Unit] =
    ZIO.accessM(_.get.start(host, port))

}
