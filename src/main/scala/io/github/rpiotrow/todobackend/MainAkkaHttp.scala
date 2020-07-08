package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.web.akkahttp.{Routes, Server}
import zio._
import zio.console.putStrLn

object MainAkkaHttp extends App {

  type AppEnvironment = ZEnv with Server

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    import BaseMain._
    val routes = (persistence ++ webConfiguration) >>> Routes.live
    val server = (routes ++ webConfiguration) >>> Server.live

    Server.server().provideSomeLayer[ZEnv](server).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(ExitCode.failure),
      _ => IO.succeed(ExitCode.success)
    )
  }

}
