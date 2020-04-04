package io.github.rpiotrow.todobackend.web

import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Api.{TodoOutput, getTodos}
import org.http4s.{EntityBody, HttpRoutes}
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import zio.{IO, RIO, Task, ZIO}

object Routes {

  def openApiRoutes(): HttpRoutes[Task] = {
    new SwaggerHttp4s(Api.openAPI).routes[Task]
  }

  implicit class ZioEndpoint[I, E, O](e: Endpoint[I, E, O, EntityBody[Task]]) {
    def toZioRoutes(logic: I => IO[E, O])(implicit serverOptions: Http4sServerOptions[Task]): HttpRoutes[Task] = {
      import sttp.tapir.server.http4s._
      e.toRoutes(i => logic(i).either)
    }
  }

  def getTodosRoute: RIO[TodoRepo, HttpRoutes[Task]] =
    ZIO.access[TodoRepo](repo =>
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
    )

}
