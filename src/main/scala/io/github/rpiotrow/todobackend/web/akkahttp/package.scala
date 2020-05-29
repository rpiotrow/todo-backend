package io.github.rpiotrow.todobackend.web

import zio.{Has, RIO, Task, ZIO, ZLayer}
import akka.http.scaladsl.server.Route
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import io.github.rpiotrow.todobackend.repository.TodoRepo
import zio.config.Config

import scala.concurrent.ExecutionContext

package object akkahttp {

  type Routes = Has[Routes.Service]
  object Routes {
    trait Service {
      def todoRoutes(implicit ec: ExecutionContext): Route
      def openApiRoutes(): Route
    }

    val live: ZLayer[TodoRepo with Config[WebConfiguration], Throwable, Routes] =
      ZLayer.fromServices[TodoRepo.Service, WebConfiguration, Routes.Service] { (repo, configuration) =>
        new AkkaHttpRoutesService(repo, configuration)
      }
  }

  type Server = Has[Server.Service]
  object Server {
    trait Service {
      def server(): Task[Nothing]
    }

    val live: ZLayer[Routes with Config[WebConfiguration], Throwable, Server] =
      ZLayer.fromServices[Routes.Service, WebConfiguration, Server.Service] { (routes, configuration) =>
        new AkkaHttpServerService(routes, configuration)
      }

    def server(): RIO[Server, Nothing] = ZIO.accessM(_.get.server())
  }

}
