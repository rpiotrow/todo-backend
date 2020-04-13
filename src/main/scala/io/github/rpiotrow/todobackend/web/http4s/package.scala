package io.github.rpiotrow.todobackend.web

import fs2.Stream
import io.github.rpiotrow.todobackend.configuration.{Configuration, WebConfiguration}
import io.github.rpiotrow.todobackend.repository.TodoRepo
import org.http4s.HttpRoutes
import zio.clock.Clock
import zio._

package object http4s {

  type Routes = Has[Routes.Service]
  object Routes {
    trait Service {
      def todoRoutes: HttpRoutes[Task]
      def openApiRoutes(): HttpRoutes[Task]
    }

    val live: ZLayer[TodoRepo with Configuration, Throwable, Routes] =
      ZLayer.fromServices[TodoRepo.Service, WebConfiguration, Routes.Service] { (repo, configuration) =>
        new Http4sRoutesService(repo, configuration)
      }
  }

  type Server = Has[Server.Service]
  object Server {
    trait Service {
      def stream: RIO[Clock, Stream[Task, Nothing]]
    }

    val live: ZLayer[Routes with Configuration, Throwable, Server] =
      ZLayer.fromServices[Routes.Service, WebConfiguration, Server.Service] { (routes, configuration) =>
        new Http4sServerService(routes, configuration)
      }

    def stream: RIO[Server with Clock, Stream[Task, Nothing]] = ZIO.accessM(_.get.stream)
  }

}
