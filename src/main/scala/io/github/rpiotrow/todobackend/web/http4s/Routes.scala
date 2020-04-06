package io.github.rpiotrow.todobackend.web.http4s

import cats.implicits._
import io.github.rpiotrow.todobackend.domain.Todo
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Api
import io.github.rpiotrow.todobackend.web.Api._
import org.http4s.{EntityBody, HttpRoutes}
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._

object Routes {

  trait Service {
    def todoRoutes: HttpRoutes[Task]
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
      private def getTodosRoute: HttpRoutes[Task] = {
        getTodos.toZioRoutes { _ =>
          repo.get
            .read().map(_.map { tuple =>
               val (id, todo) = tuple
               output(todo, id)
            })
            .mapError { _.getMessage() }
        }
      }
      private def getTodoRoute: HttpRoutes[Task] = {
        getTodo.toZioRoutes { id =>
          repo.get
            .read(id).map { option =>
              option match {
                case Some(todo) => (output(todo, id).some, StatusCode.Ok)
                case None       => (None, StatusCode.NotFound)
              }
            }.mapError(_.getMessage())
        }
      }
      private def createTodoRoute: HttpRoutes[Task] = {
        createTodo.toZioRoutes { input =>
          val todo = Todo(
            title = input.title,
            completed = false
          )
          repo.get
            .insert(todo).map(todoUrl(_))
            .mapError(_.getMessage())
        }
      }
      private def updateTodoRoute: HttpRoutes[Task] = {
        updateTodo.toZioRoutes { tuple =>
          val (id, input) = tuple
          val todo = Todo(
            title = input.title,
            completed = input.completed
          )
          repo.get
            .update(id, todo).map(_ => output(todo, id))
            .mapError(_.getMessage())
        }
      }
      private def deleteTodoRoute: HttpRoutes[Task] = {
        deleteTodo.toZioRoutes { id =>
          repo.get
            .delete(id)
            .mapError(_.getMessage())
        }
      }

      private def todoUrl(id: Long) = s"http://localhost:8080/todos/${id}"
      private def output(todo: Todo, id: Long) = TodoOutput(
        title = todo.title,
        url = todoUrl(id),
        completed = todo.completed
      )

      def todoRoutes: HttpRoutes[Task] =
        createTodoRoute <+> getTodosRoute <+> getTodoRoute <+> updateTodoRoute <+> deleteTodoRoute

      def openApiRoutes(): HttpRoutes[Task] = {
        new SwaggerHttp4s(Api.openAPI).routes[Task]
      }
    }
  )

  def todoRoutes: RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.todoRoutes)
  def openApiRoutes(): RIO[Routes, HttpRoutes[Task]] = ZIO.access(_.get.openApiRoutes)

}
