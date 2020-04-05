package io.github.rpiotrow.todobackend.web.http4s

import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Api
import io.github.rpiotrow.todobackend.web.Api.{TodoOutput, getTodos}
import org.http4s.{EntityBody, HttpRoutes}
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import zio.{IO, RIO, Task, ZIO, _}

object Routes {

  trait Service {
    def getTodosRoute: HttpRoutes[Task]
    def openApiRoutes(): HttpRoutes[Task]
  }

  implicit class ZioEndpoint[I, E, O](e: Endpoint[I, E, O, EntityBody[Task]]) {
    def toZioRoutes(logic: I => IO[E, O])(implicit serverOptions: Http4sServerOptions[Task]): HttpRoutes[Task] = {
      import sttp.tapir.server.http4s._
      e.toRoutes(i => logic(i).either)
    }
  }

  val live: ZLayer[TodoRepo, Nothing, Routes] = ZLayer.fromFunction( repo =>
    new Service {
      def getTodosRoute: HttpRoutes[Task] = {
        getTodos.toZioRoutes { _ =>
          repo.get.getAll().map { list =>
            list.map { e =>
              TodoOutput(
                title = e.title,
                url = s"http://localhost:8080/todos/${e.id}",
                completed = e.completed
              )
            }
          }.mapError { _.getMessage() }
        }
      }
      def openApiRoutes(): HttpRoutes[Task] = {
        new SwaggerHttp4s(Api.openAPI).routes[Task]
      }
    }
  )

  def getTodosRoute: RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.getTodosRoute)
  def openApiRoutes(): RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.openApiRoutes)

}
