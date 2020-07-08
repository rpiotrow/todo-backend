package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.web.http4s.{Routes, Server}
import zio._
import zio.console.putStrLn
import zio.interop.catz._

object MainHttp4s extends CatsApp {

  type AppEnvironment = ZEnv with Server

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    import BaseMain._
    val routes = (persistence ++ webConfiguration) >>> Routes.live
    val server = (routes ++ webConfiguration) >>> Server.live

    val program: RIO[AppEnvironment, Unit] =
      for {
        serverStream <- Server.stream
        server <- serverStream.compile[Task, Task, ExitCode].drain
      } yield server
    program.provideSomeLayer[ZEnv](server).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(ExitCode.failure),
      _ => IO.succeed(ExitCode.success)
    )
  }

}
