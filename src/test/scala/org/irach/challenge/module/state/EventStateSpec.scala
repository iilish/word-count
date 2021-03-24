package org.irach.challenge.module.state

import org.irach.challenge.domain
import org.irach.challenge.domain.Event
import org.irach.challenge.module.mock.RepositoryMock
import org.irach.challenge.module.repo.Repository
import zio.logging.{LogContext, Logger}
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.mock.Expectation
import zio.test.mock.Expectation.value
import zio.{Ref, Task, UIO, ZIO, ZLayer}

object EventStateSpec extends DefaultRunnableSpec {

  private val mockRepo = ZLayer.succeed {
    new Repository.Service {
      override def writeAll(map: Map[String, Long]): Task[Unit] = ZIO.succeed()

      override def readAll(): Task[List[domain.Event]] = ZIO.succeed(List())

      override def write(key: String, value: Long): Task[Unit] = ZIO.succeed()

      override def read(key: String): Task[Option[domain.Event]] = ZIO.none
    }
  }
  private val mockLogging = ZLayer.succeed {
    new Logger[String] {
      override def locally[R1, E, A1](f: LogContext => LogContext)(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1] = ???

      override def log(line: => String): UIO[Unit] = ???

      override def logContext: UIO[LogContext] = ???

      override def debug(line: => String): UIO[Unit] = ZIO.succeed()

      override def warn(line: => String): UIO[Unit] = ZIO.succeed()

      override def info(line: => String): UIO[Unit] = ZIO.succeed(())
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("EventState")(
      testM("increment add 1 to counter") {
        // GIVEN
        val eventType = "foo"
        val counterValue = 45L
        val ref = Ref.make(Map[String, Long](eventType -> counterValue))
        val env = ref.toLayer ++ mockRepo ++ mockLogging >>> EventState.live

        // WHEN
        val result = EventState.increment(eventType).provideLayer(env)

        // THEN
        assertM(result)(equalTo(46L))
      },

      testM("initialize set empty map") {
        // GIVEN
        val ref = Ref.make(Map[String, Long]("foo" -> 45L))
        val env = ref.toLayer ++ mockRepo ++ mockLogging >>> EventState.live

        // WHEN
        val result = EventState.initialize.provideLayer(env)

        // THEN
        assertM(result)(equalTo(Map[String, Long]()))
      },

      testM("persist delegates to repo.writeAll") {
        // GIVEN
        val state = Map("foo" -> 45L)
        val repoLayer = Expectation.toLayer(RepositoryMock.WriteAll(equalTo(state)))
        val env = Ref.make(state).toLayer ++ repoLayer ++ mockLogging >>> EventState.live

        // WHEN
        val result = EventState.persist().provideLayer(env)

        // THEN
        assertM(result)(equalTo(()))
      },

      testM("load delegates to repo.readAll") {
        // GIVEN
        val dbStates = List(Event("foo" , 45L), Event("bar" , 65L), Event("baz" , 25L))
        val expectedState = dbStates.map(event => (event.eventType, event.count)).toMap
        val repoLayer = Expectation.toLayer(RepositoryMock.ReadAll(value(dbStates)))
        val env = Ref.make(Map[String, Long]()).toLayer ++ repoLayer ++ mockLogging >>> EventState.live

        // WHEN
        val result = EventState.load().provideLayer(env)

        // THEN
        assertM(result)(equalTo(expectedState))
      }
  )
}
