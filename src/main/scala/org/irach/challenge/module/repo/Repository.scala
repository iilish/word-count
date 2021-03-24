package org.irach.challenge.module.repo

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.{H2JdbcContext, SnakeCase}
import org.irach.challenge.domain.Event
import org.irach.challenge.environment.config.Configuration.DBConfig
import zio.macros.accessible
import zio.{Has, Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

@accessible
object Repository {
  type Repository = Has[Repository.Service]
  type DBCtx = H2JdbcContext[SnakeCase]
  type Resp = List[Event]

  trait Service {
    def writeAll(map: Map[String, Long]): Task[Unit]

    def readAll(): Task[List[Event]]

    def write(key: String, value: Long): Task[Unit]

    def read(key: String): Task[Option[Event]]
  }

  val live: ZLayer[Has[DBConfig], Nothing, Has[Repository.Service]] =
    ZLayer.fromService[DBConfig, Repository.Service] {
      cfg =>
        new Service {
          lazy val ctx: DBCtx = new H2JdbcContext(SnakeCase, getConfig)

          import ctx._

          val currentDirectory: String = new java.io.File(".").getAbsolutePath
          val connectionString = s"jdbc:h2:$currentDirectory/cache"

          private def getConfig: Config = {
            lazy val configMap = Map(
              "dataSourceClassName" -> "org.h2.jdbcx.JdbcDataSource",
              "dataSource.url" -> connectionString,
              "dataSource.user" -> cfg.user,
              "dataSource.password" -> cfg.pass
            ).asJava

            ConfigFactory.parseMap(configMap)
          }

          // FIXME: use upsert, change DB ??
          def write(eventType: String, count: Long): Task[Unit] = ZIO.effect {
            Option(run(query[Event].filter(_.eventType == lift(eventType)).delete))
              .map(_ => run(query[Event].insert(lift(Event(eventType, count)))))
          }

          // TODO: Optimize upsert, change DB ???
          def writeAll(state: Map[String, Long]): Task[Unit] = for {
            _ <- ZIO.effect(
              run(liftQuery(state.toList).foreach {
                pair => query[Event].filter(_.eventType == pair._1).delete
              })).when(state.nonEmpty)
            _ <- ZIO.effect(
              run(
                liftQuery(state.toList).foreach {
                  pair => {
                    query[Event].insert(_.eventType -> pair._1, _.count -> pair._2)
                  }
                }
              )
            ).when(state.nonEmpty)
          } yield ()

          def read(eventType: String): Task[Option[Event]] = ZIO.effect(
            run(query[Event].filter(_.eventType == lift(eventType))).headOption
          )

          def readAll(): Task[Resp] = ZIO.effect(run(query[Event]))

        }
    }
}

