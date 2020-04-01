package io.github.rpiotrow.todobackend.web

import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zio.clock.Clock
import zio.interop.catz._
import zio.{RIO, Runtime}

object Server {

  def stream[R <: Clock](implicit runtime: Runtime[R]): Stream[RIO[R, *], Nothing] = {
    val httpApp = Routes.openApiRoutes[R].orNotFound
    val httpAppWithLogging = Logger.httpApp(true, true)(httpApp)
    BlazeServerBuilder[RIO[R, *]]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpAppWithLogging)
      .serve
  }.drain

}
