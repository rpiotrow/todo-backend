package io.github.rpiotrow.todobackend

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.github.rpiotrow.todobackend.web.Server

object Main extends IOApp {

  def run(args: List[String]) =
    Server.stream[IO].compile.drain.as(ExitCode.Success)

}
