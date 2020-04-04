package io.github.rpiotrow.todobackend.web

import fs2.Stream
import io.github.rpiotrow.todobackend.repository.TodoRepo
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{RIO, Runtime, Task, ZEnv}

object Server {

  type ServerEnvironment = TodoRepo with Clock

  def stream(implicit r: Runtime[ZEnv]): RIO[ServerEnvironment, Stream[Task, Nothing]] = {
    for {
      routes <- Routes.getTodosRoute
      httpApp = routes.orNotFound
      httpAppWithLogging = Logger.httpApp(true, true)(httpApp)
      server = BlazeServerBuilder[Task]
        .bindHttp(8080, "localhost")
        .withHttpApp(httpAppWithLogging)
        .serve
      result = server.drain
    } yield result
  }

}
