package io.github.rpiotrow.todobackend.web.http4s

import cats.implicits._
import fs2.Stream
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{RIO, Task, ZIO}

class Http4sServerService(
  routes: Routes.Service,
  configuration: WebConfiguration
) extends Server.Service {

  override def stream: RIO[Clock, Stream[Task, Nothing]] = ZIO.runtime[Clock].flatMap { implicit runtime =>
    val httpApp = (routes.todoRoutes <+> routes.openApiRoutes).orNotFound
    val httpAppWithLogging = Logger.httpApp(true, true)(httpApp)
    ZIO.descriptor.map { d =>
      val server = BlazeServerBuilder[Task](d.executor.asEC)
        .bindHttp(configuration.port, configuration.host)
        .withHttpApp(httpAppWithLogging)
        .serve
      server.drain
    }
  }

}
