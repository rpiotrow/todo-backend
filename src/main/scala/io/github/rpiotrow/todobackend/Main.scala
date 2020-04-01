package io.github.rpiotrow.todobackend

import cats.effect.ExitCode
import io.github.rpiotrow.todobackend.web.Server
import zio._
import zio.clock.Clock
import zio.console.putStrLn
import zio.interop.catz._

object Main extends App {

  type AppEnvironment = Clock
  type AppTask[A] = RIO[AppEnvironment, A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val program: ZIO[ZEnv, Throwable, Unit] =
      ZIO.runtime[AppEnvironment].flatMap { implicit runtime =>
        Server
          .stream
          .compile[AppTask, AppTask, ExitCode]
          .drain
      }
    program.foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _ => IO.succeed(0)
    )
  }

}
