package org.irach

import org.irach.challenge.environment.config.Configuration.Configuration
import org.irach.challenge.module.accumulator.Accumulator.Accumulator
import org.irach.challenge.module.api.APIServer.APIServer
import org.irach.challenge.module.blackbox.BlackBoxLauncher.BlackBoxLauncher
import org.irach.challenge.module.parser.EventParser.EventParser
import org.irach.challenge.module.repo.Repository.Repository
import org.irach.challenge.module.source.SocketSource.SocketSource
import org.irach.challenge.module.state.EventState.EventState
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging

package object challenge {
  type WordCountEnv = Clock
    with Blocking
    with Console
    with Configuration
    with SocketSource
    with EventParser
    with Logging
    with Accumulator
    with EventState
    with APIServer
    with Repository
    with BlackBoxLauncher


}
