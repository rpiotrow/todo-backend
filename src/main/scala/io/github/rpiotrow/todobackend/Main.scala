package io.github.rpiotrow.todobackend

import cats.effect.ExitCode
import io.github.rpiotrow.todobackend.configuration.Configuration
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.http4s.{Routes, Server}
import zio._
import zio.console.putStrLn
import zio.interop.catz._
import zio.blocking.Blocking

object Main extends CatsApp {

  type AppEnvironment = ZEnv with Server

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val program: RIO[AppEnvironment, Unit] =
      for {
        serverStream <- Server.stream
        server <- serverStream.compile[Task, Task, ExitCode].drain
      } yield server
    val configuration = Configuration.live
    val persistence = (configuration ++ Blocking.live) >>> TodoRepo.postgreSQL(platform.executor.asEC)
    val routes = (persistence ++ configuration) >>> Routes.live
    val server = (routes ++ configuration) >>> Server.live
    program.provideSomeLayer[ZEnv](server).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _ => IO.succeed(0)
    )
  }

}
