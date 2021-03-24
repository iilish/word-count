package org.irach.challenge.module.source

import org.irach.challenge.domain.ApplicationEvent
import org.irach.challenge.module.accumulator.Accumulator.Accumulator
import org.irach.challenge.module.parser.EventParser.{EventParser, decodeLine}
import zio.blocking.Blocking
import zio.console.Console
import zio.macros.accessible
import zio.stream.{ZStream, ZTransducer}
import zio.{Has, ULayer, ZLayer}

@accessible
object SocketSource {
  type SocketSource = Has[Service]

  trait Service {
    def rawStream(host: String, port: Int, numOfConnection: Int): ZStream[EventParser with Accumulator with Blocking with Console, Throwable, ApplicationEvent]
  }

  private def parseChunk(stringStream: ZStream[Any, Throwable, String], appId: Int): ZStream[EventParser, Throwable, ApplicationEvent] =
    stringStream.flatMap(blockString => ZStream.fromIterable(blockString.split("\n").toList))
      .flatMap(jsonLine => ZStream.fromEffect(decodeLine(appId, jsonLine)))

  lazy val live: ULayer[Has[Service]] = ZLayer.succeed((host: String, port: Int, numOfConnection: Int) => {
    ZStream
      .fromSocketServer(port, Some(host))
      .flatMapPar(numOfConnection)(
        conn =>
          //FIXME: The application id comes from the socket client ???
          // Find a way to dynamically associate a connection with an application to recovery
          parseChunk(conn.read.transduce(ZTransducer.utf8Decode), conn.hashCode())
      )
  })
}