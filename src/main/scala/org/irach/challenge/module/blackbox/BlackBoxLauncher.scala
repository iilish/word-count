package org.irach.challenge.module.blackbox

import org.irach.challenge.environment.config.Configuration.BlackBoxConfig
import zio.macros.accessible
import zio.{Has, IO, Task, ZIO, ZLayer}

import scala.sys.process._

@accessible
object BlackBoxLauncher {
  type BlackBoxLauncher = Has[BlackBoxLauncher.Service]

  trait Service {
    def launch(conf: BlackBoxConfig): Task[String]
  }

  lazy val live: ZLayer[Any, Throwable, Has[Service]] =
    ZIO.effect(
      new Service {
        override def launch(conf: BlackBoxConfig): IO[Throwable, String] =
          Task((s"${conf.appPath}" #| s"${conf.netcatCmd}").!!)
      }
    ).toLayer
}
