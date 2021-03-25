package org.irach.challenge

import org.irach.challenge.domain.{Strategy, TimeTumbling, Watermark}
import org.irach.challenge.environment.config.Configuration
import org.irach.challenge.environment.config.Configuration.{APIConfig, BlackBoxConfig, SocketConfig}
import org.irach.challenge.module.accumulator.Accumulator
import org.irach.challenge.module.accumulator.Accumulator.aggregate
import org.irach.challenge.module.api.APIServer
import org.irach.challenge.module.api.APIServer.start
import org.irach.challenge.module.blackbox.BlackBoxLauncher
import org.irach.challenge.module.blackbox.BlackBoxLauncher.launch
import org.irach.challenge.module.parser.EventParser
import org.irach.challenge.module.repo.AppManager.initializeApplication
import org.irach.challenge.module.repo.{AppManager, Repository}
import org.irach.challenge.module.source.SocketSource
import org.irach.challenge.module.source.SocketSource.rawStream
import org.irach.challenge.module.state.{EventState, InternalState}
import zio._
import zio.clock.Clock
import zio.duration.{durationInt, durationLong}
import zio.logging.Logging.info
import zio.logging.slf4j.Slf4jLogger
import zio.stream.Stream

object WordCountApp extends zio.App {

  import CommandParser._

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    program(args).provideCustomLayer(appLayer).exitCode
  }

  private def program(args: List[String]) = for {
    config <- Configuration.live.build.useNow
    (socketConf, apiConf, blackboxConf) <- UIO.succeed(buildConfiguration(
      args, (config.get[SocketConfig], config.get[APIConfig], config.get[BlackBoxConfig])
    ))
    _ <- info("initializing services") *>
      initializeApplication(socketConf) <*
      info("Starting word count process...")

    socketFiber <- info(s"***** socketConfig => [$socketConf]") *> rawStream(socketConf.host, socketConf.port, socketConf.numberOfConnections)
      .merge(Stream(Watermark(1)).repeat(Schedule.spaced(socketConf.watermarkInterval.milliseconds)))
      .mapM(aggregate)
      .runDrain
      .when(socketConf.runFlag)
      .fork

    apiFiber <- (info(s"***** apiConfig => [$apiConf]") *> start(apiConf.host, apiConf.port))
      .when(apiConf.runFlag)
      .fork

    blackboxFiber <- (info("Waiting socket server startup before launching blackbox.amd64 ...") *>
      ZIO.sleep(blackboxConf.waitInSeconds.seconds) *>
      info(s"***** blackboxConfig => [$blackboxConf]") *> launch(blackboxConf))
      .when(blackboxConf.runFlag)
      .fork

    _ <- info("Up and running!")
    syntheticFiber = apiFiber <*> socketFiber <*> blackboxFiber
    _ <- syntheticFiber.join
  } yield ()

  private lazy val appLayer = {
    val loggingLayer = Slf4jLogger.make { (_, message) => "%s".format(message) }
    val dbLayer = Configuration.live >>> Repository.live
    val stateLayer = Ref.make(Map[String, Long]()).toLayer ++ dbLayer ++ loggingLayer >>> EventState.live
    val internalStateLayer = Ref.make(TimeTumbling().asInstanceOf[Strategy]).toLayer ++ loggingLayer >>> InternalState.live
    val accumulatorLayer = (loggingLayer ++ stateLayer ++ internalStateLayer) >>> Accumulator.live
    val apiServerLayer = (ZLayer.identity[Clock] ++ dbLayer) >>> APIServer.live
    val DBLayer = (internalStateLayer ++ loggingLayer) >>> AppManager.live
    dbLayer ++ SocketSource.live ++ EventParser.live ++ accumulatorLayer ++ stateLayer ++ apiServerLayer ++
      BlackBoxLauncher.live ++ Configuration.live ++ loggingLayer ++ DBLayer
  }


}
