package org.irach.challenge.environment.config

import org.irach.challenge.domain
import org.irach.challenge.domain.{EventCount, EventTime, TimeTumbling}
import pureconfig.ConfigSource
import zio.{Has, ULayer, ZIO}

object Configuration {
  type Configuration = Has[SocketConfig] with Has[DBConfig] with Has[APIConfig] with Has[BlackBoxConfig]

  case class SocketConfig(host: String, port: Int, numberOfConnections: Int, windowStrategy: String = "watermark",
                          watermarkInterval: Long = 5000, eventCountLimit: Int = 20, eventTimeLimit: Long = 3000,
                          runFlag: Boolean = true) {
    def buildStrategy: domain.Strategy = windowStrategy.toLowerCase match {
      case "watermark" => TimeTumbling()
      case "event-count" => EventCount(eventCountLimit)
      case "event-time" => EventTime(eventTimeLimit, 0, 0)
      case _ => TimeTumbling()
    }
  }

  case class DBConfig(user: String, pass: String)

  case class APIConfig(host: String, port: Int, runFlag: Boolean = true)

  case class BlackBoxConfig(appPath: String, netcatCmd: String, waitInSeconds: Int = 4, runFlag: Boolean = true)

  case class AppConfig(socketServer: SocketConfig, db: DBConfig, apiServer: APIConfig, blackbox: BlackBoxConfig)

  val live: ULayer[Configuration] = ZIO.effect {
    import pureconfig.generic.auto._
    ConfigSource.default.loadOrThrow[AppConfig]
  }.map(c => {
    Has(c.socketServer) ++ Has(c.db) ++ Has(c.apiServer) ++ Has(c.blackbox)
  })
    .orDie
    .toLayerMany

}