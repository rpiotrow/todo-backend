package io.github.rpiotrow.todobackend

import cats.effect.ExitCode
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Server
import zio._
import zio.console.putStrLn
import zio.interop.catz._

object Main extends CatsApp {

  type AppEnvironment = ZEnv with TodoRepo

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val program: RIO[AppEnvironment, Unit] =
      for {
        serverStream <- Server.stream
        server <- serverStream.compile[Task, Task, ExitCode].drain
      } yield server
    program.provideSomeLayer[ZEnv](TodoRepo.live).foldM(
      err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1),
      _ => IO.succeed(0)
    )
  }

}
