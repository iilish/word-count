package org.irach.challenge.module

import zio.logging.{LogContext, Logger, Logging}
import zio.test.mock.{Mock, Proxy}
import zio._

object LoggingMock extends Mock[Logging] {

  object Debug extends Effect[String, Nothing, Unit]

  object Warn extends Effect[String, Nothing, Unit]

  object Info extends Effect[String, Nothing, Unit]

  override val compose: URLayer[Has[Proxy], Logging] =
    ZLayer.fromService(proxy =>
      new Logger[String] {
        override def locally[R1, E, A1](f: LogContext => LogContext)(zio: ZIO[R1, E, A1]): ZIO[R1, E, A1] = ???

        override def log(line: => String): UIO[Unit] = ???

        override def logContext: UIO[LogContext] = ???

        override def debug(line: => String): UIO[Unit] = proxy(Debug, line)

        override def warn(line: => String): UIO[Unit] = proxy(Warn, line)

        override def info(line: => String): UIO[Unit] = proxy(Info, line)
      })
}

/*
*object MockConsole extends Mock[Console] {

  object PutStr      extends Effect[String, Nothing, Unit]
  object PutStrErr   extends Effect[String, Nothing, Unit]
  object PutStrLn    extends Effect[String, Nothing, Unit]
  object PutStrLnErr extends Effect[String, Nothing, Unit]
  object GetStrLn    extends Effect[Unit, IOException, String]

  val compose: URLayer[Has[Proxy], Console] =
    ZLayer.fromService(proxy =>
      new Console.Service {
        def putStr(line: String): UIO[Unit]      = proxy(PutStr, line)
        def putStrErr(line: String): UIO[Unit]   = proxy(PutStrErr, line)
        def putStrLn(line: String): UIO[Unit]    = proxy(PutStrLn, line)
        def putStrLnErr(line: String): UIO[Unit] = proxy(PutStrLnErr, line)
        val getStrLn: IO[IOException, String]    = proxy(GetStrLn)
      }
    )
} */