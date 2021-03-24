package org.irach.challenge

import org.irach.challenge.environment.config.Configuration.{APIConfig, BlackBoxConfig, SocketConfig}
import scopt.OParser


object CommandParser {

  def buildConfiguration(args: List[String], fromFileConfiguration: (SocketConfig, APIConfig, BlackBoxConfig))
  : (SocketConfig, APIConfig, BlackBoxConfig) =
    merge(OParser.parse(parser, args, WordCountConfig()), fromFileConfiguration)

  private def merge(cmdConf: Option[WordCountConfig],
                    appConf: (SocketConfig, APIConfig, BlackBoxConfig)): (SocketConfig, APIConfig, BlackBoxConfig) =
    cmdConf.map { conf =>
      (
        assignSocket(conf, appConf._1),
        assignApi(conf, appConf._2),
        assignBlackbox(conf, appConf._3)
      )
    }.getOrElse(appConf)

  private def orElse[T](value: Option[T], default: T): T = value.getOrElse(default)

  private def assignSocket(wc: WordCountConfig, socketConf: SocketConfig): SocketConfig = {
    val host = orElse[String](wc.socketServerHost, socketConf.host)
    val port = orElse[Int](wc.socketServerPort, socketConf.port)
    val connections = orElse[Int](wc.socketServerNomOfConn, socketConf.numberOfConnections)
    val windowStrategy = orElse[String](wc.windowStrategy, socketConf.windowStrategy)
    val watermarkInterval = orElse[Long](wc.watermarkInterval, socketConf.watermarkInterval)
    val eventCountLimit = orElse[Int](wc.eventCountLimit, socketConf.eventCountLimit)
    val eventTimeLimit = orElse[Long](wc.eventTimeLimit, socketConf.eventTimeLimit)
    val runFlag = !wc.modeActivated || (wc.modeActivated && wc.mode.contains("socket-server"))
    SocketConfig(host, port, connections, windowStrategy, watermarkInterval, eventCountLimit, eventTimeLimit, runFlag)
  }

  private def assignApi(wc: WordCountConfig, apiConf: APIConfig): APIConfig = {
    val host = orElse[String](wc.apiServerHost, apiConf.host)
    val port = orElse[Int](wc.apiServerPort, apiConf.port)
    val runFlag = !wc.modeActivated || (wc.modeActivated && wc.mode.contains("api-server"))
    APIConfig(host, port, runFlag)
  }

  private def assignBlackbox(wc: WordCountConfig, blackboxConf: BlackBoxConfig): BlackBoxConfig = {
    val path = orElse[String](wc.blackboxPath, blackboxConf.appPath)
    val netcatCmd = orElse[String](wc.blackboxNetcatCmd, blackboxConf.netcatCmd)
    val runFlag = !wc.modeActivated || (wc.modeActivated && wc.mode.contains("blackbox"))
    BlackBoxConfig(path, netcatCmd, blackboxConf.waitInSeconds, runFlag)
  }

  case class WordCountConfig( // Socket server configuration
                              socketServerHost: Option[String] = None, socketServerPort: Option[Int] = None,
                              socketServerNomOfConn: Option[Int] = None, windowStrategy: Option[String] = None,
                              watermarkInterval: Option[Long] = None, eventCountLimit: Option[Int] = None,
                              eventTimeLimit: Option[Long] = None,
                              // API server configuration
                              apiServerHost: Option[String] = None, apiServerPort: Option[Int] = None,
                              // Blackbox configuration
                              blackboxPath: Option[String] = None, blackboxNetcatCmd: Option[String] = None,
                              waitInSeconds: Option[Int] = None,
                              // Mode Configuration
                              modeActivated: Boolean = false, mode: Seq[String] = Seq())

  private def parser = {
    import scopt.OParser
    val builder = OParser.builder[WordCountConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("blackbox word count"),
        head("BlackBox Word Count", "1.0"),
        opt[Option[String]]('s', "socket-server-host")
          .action((ssHost, c) => c.copy(socketServerHost = ssHost))
          .text("Machine name for the socket server to run"),
        opt[Option[Int]]('t', "socket-server-port")
          .action((ssPort, c) => c.copy(socketServerPort = ssPort))
          .text("Machine port for the socket server to run"),
        opt[Option[Int]]('u', "socket-server-n-conn")
          .action((ssNConn, c) => c.copy(socketServerNomOfConn = ssNConn))
          .text("Number of connections"),
        opt[Option[String]]('v', "socket-server-window-strategy")
          .action((windowStrategy, c) => c.copy(windowStrategy = windowStrategy))
          .text("Window strategy to use [`watermark` | event-count | event-time]"),
        opt[Option[Long]]('w', "socket-server-watermark-interval")
          .action((interval, c) => c.copy(watermarkInterval = interval))
          .text("Interval time in milliseconds"),
        opt[Option[Int]]('x', "socket-server-event-count-limit")
          .action((limit, c) => c.copy(eventCountLimit = limit))
          .text("Limit count for events by window"),
        opt[Option[Long]]('y', "socket-server-event-time-limit")
          .action((limit, c) => c.copy(eventTimeLimit = limit))
          .text("Event time size in milliseconds by window"),
        opt[Option[String]]('a', "api-server-host")
          .action((apiHost, c) => c.copy(apiServerHost = apiHost))
          .text("Machine name for the api server to run"),
        opt[Option[Int]]('b', "api-server-port")
          .action((apiPort, c) => c.copy(apiServerPort = apiPort))
          .text("Machine port for the api  server to run"),
        opt[Option[String]]('n', "blackbox-path")
          .action((blackboxPath, c) => c.copy(blackboxPath = blackboxPath))
          .text("Full path of the binary application [blackbox.amd64]"),
        opt[Option[String]]('o', "blackbox-netcat-cmd")
          .action((blackboxNC, c) => c.copy(blackboxNetcatCmd = blackboxNC))
          .text("The netcat command to use "),
        help("help").text("prints this usage text"),
        note("All the options are optionals" + sys.props("line.separator")),
        cmd("mode")
          .action((_, c) => c.copy(modeActivated = true))
          .text("You can run in 3 modes [socket-server | blackbox | apî-server].")
          .children(
            opt[Boolean]("socket-server")
              .abbr("ss")
              .action((isSet, c) => if (isSet) c.copy(mode = c.mode ++ Seq("socket-server")) else c)
              .text("only run socket server streaming")
            ,
            opt[Boolean]("api-server")
              .abbr("api")
              .action((isSet, c) => if (isSet) c.copy(mode = c.mode ++ Seq("api-server")) else c)
              .text("only run api server")
            ,
            opt[Boolean]("blackbox")
              .abbr("bb")
              .action((isSet, c) => if (isSet) c.copy(mode = c.mode ++ Seq("blackbox")) else c)
              .text("only run blackbox application")
          ),
        checkConfig(
          c =>
            c.windowStrategy.fold(success)(
              strategy =>
              if (Seq("watermark", "event-count", "event-time").contains(strategy.toLowerCase())) success
              else failure("xyz cannot keep alive")
            )
        )
      )
    }
    parser
  }

}
