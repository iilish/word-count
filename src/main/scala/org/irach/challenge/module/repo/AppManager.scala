package org.irach.challenge.module.repo

import org.irach.challenge.environment.config.Configuration.SocketConfig
import org.irach.challenge.module.state.InternalState
import org.irach.challenge.module.state.InternalState.InternalState
import zio._
import zio.logging.{Logger, Logging}
import zio.macros.accessible

import java.sql.{Connection, DriverManager}

@accessible
object AppManager {
  type AppManager = Has[Service]
  val sqlInitialization =
    """
          CREATE TABLE IF NOT EXISTS event (
            event_type VARCHAR(50) NOT NULL,
            count BIGINT NOT NULL)
          """

  trait Service {
    def initializeApplication(socketConfig: SocketConfig): Task[Boolean]
  }

  private def connection(): Task[Connection] = Task[Connection] {
    Class.forName("org.h2.Driver")
    val currentDirectory = new java.io.File(".").getAbsolutePath
    val connectionString = s"jdbc:h2:$currentDirectory/cache"
    DriverManager.getConnection(connectionString)
  }

  private lazy val zConnection: ZLayer[Any, Throwable, Has[Connection]] =
    ZLayer.fromAcquireRelease(connection())(c => UIO(c.close()))

  private lazy val zDB: ZLayer[Has[Connection] with Logging with InternalState, Nothing, AppManager] =
    ZLayer.fromServices[Connection, Logger[String], InternalState.Service, AppManager.Service]((conn, log, state) => (socketConfig: SocketConfig) => {
      for {
        created <- ZIO.effect {
          conn.createStatement().execute(sqlInitialization)
        }
        _ <- state.setStrategy(socketConfig.buildStrategy)
        _ <- log.info(s"Initialization done ")
      } yield created

    })

  lazy val live: ZLayer[Logging with InternalState, Throwable, AppManager] =
    (ZLayer.identity[InternalState] ++ ZLayer.identity[Logging] ++ zConnection) >>> zDB
}
