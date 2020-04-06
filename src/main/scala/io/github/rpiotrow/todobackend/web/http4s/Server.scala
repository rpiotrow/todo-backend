package io.github.rpiotrow.todobackend.web.http4s

import cats.implicits._
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{RIO, Task, ZIO, ZLayer}

object Server {

  trait Service {
    def stream: RIO[Clock, Stream[Task, Nothing]]
  }

  val live: ZLayer[Routes, Nothing, Server] = ZLayer.fromFunction { routes =>
    new Service {
      override def stream: RIO[Clock, Stream[Task, Nothing]] = ZIO.runtime[Clock].map { implicit runtime =>
        val httpApp = (routes.get.todoRoutes <+> routes.get.openApiRoutes).orNotFound
        val httpAppWithLogging = Logger.httpApp(true, true)(httpApp)
        val server = BlazeServerBuilder[Task]
          .bindHttp(8080, "localhost")
          .withHttpApp(httpAppWithLogging)
          .serve
        server.drain
      }
    }
  }

  def stream(): RIO[Server with Clock, Stream[Task, Nothing]] = ZIO.accessM(_.get.stream)

}
