package io.github.rpiotrow.todobackend.web

import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.RIO
import zio.interop.catz._

object Routes {

  def openApiRoutes[R](): HttpRoutes[RIO[R, *]] = {
    new SwaggerHttp4s(Api.openAPI).routes[RIO[R, *]]
  }

}
