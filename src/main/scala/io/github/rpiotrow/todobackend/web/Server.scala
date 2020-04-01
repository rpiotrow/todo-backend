package io.github.rpiotrow.todobackend.web

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    val httpApp = Routes.openApiRoutes.orNotFound
    val httpAppWithLogging = Logger.httpApp(true, true)(httpApp)
    BlazeServerBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpAppWithLogging)
      .serve
  }.drain

}
