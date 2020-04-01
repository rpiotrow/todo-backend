package io.github.rpiotrow.todobackend.web

import cats.effect.{ContextShift, Sync}
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object Routes {

  def openApiRoutes[F[_]: ContextShift: Sync](): HttpRoutes[F] = {
    new SwaggerHttp4s(Api.openAPI).routes
  }

}
