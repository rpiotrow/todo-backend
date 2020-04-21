package io.github.rpiotrow.todobackend.web.http4s

import cats.implicits._
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
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

class Http4sRoutesService(repo: TodoRepo.Service, configuration: WebConfiguration) extends Routes.Service {

  implicit class ZioEndpoint[I, E, O](e: Endpoint[I, E, O, EntityBody[Task]]) {
    def toZioRoutes(logic: I => IO[E, O])(implicit serverOptions: Http4sServerOptions[Task]): HttpRoutes[Task] = {
      import sttp.tapir.server.http4s._
      e.toRoutes(i => logic(i).either)
    }
  }

  private def getTodosRoute: HttpRoutes[Task] = {
    getTodos.toZioRoutes { _ =>
      repo
        .read().map(_.map { case (id, todo) => output(todo, id) })
        .mapError(_.getMessage())
    }
  }
  private def getTodoRoute: HttpRoutes[Task] = {
    getTodo.toZioRoutes { id =>
      repo
        .read(id).map {
          case Some(todo) => (output(todo, id).some, StatusCode.Ok)
          case None       => (None, StatusCode.NotFound)
        }.mapError(_.getMessage())
    }
  }
  private def createTodoRoute: HttpRoutes[Task] = {
    createTodo.toZioRoutes { input =>
      val todo = Todo(
        title = input.title,
        completed = false
      )
      repo
        .insert(todo).map(todoUrl)
        .mapError(_.getMessage())
    }
  }
  private def updateTodoRoute(): HttpRoutes[Task] = {
    updateTodo.toZioRoutes { tuple =>
      val (id, input) = tuple
      val todo = Todo(
        title = input.title,
        completed = input.completed
      )
      repo
        .update(id, todo).map {
          case Some(_) => (output(todo, id).some, StatusCode.Ok)
          case None    => (None, StatusCode.NotFound)
        }.mapError(_.getMessage())
    }
  }
  private def patchTodoRoute: HttpRoutes[Task] = {
    patchTodo.toZioRoutes { case (id, input) =>
      repo
        .update(id, input.title, input.completed).map {
          case Some(todo) => (output(todo, id).some, StatusCode.Ok)
          case None       => (None, StatusCode.NotFound)
        }.mapError(_.getMessage())
    }
  }
  private def deleteTodoRoute(): HttpRoutes[Task] = {
    deleteTodo.toZioRoutes { id =>
      repo
        .delete(id).map {
          case Some(_) => StatusCode.NoContent
          case None    => StatusCode.NotFound
        }.mapError(_.getMessage())
    }
  }

  private def todoUrl(id: Long) = s"http://${configuration.host}:${configuration.port}/todos/${id}"
  private def output(todo: Todo, id: Long) = TodoOutput(
    title = todo.title,
    url = todoUrl(id),
    completed = todo.completed
  )

  override def todoRoutes: HttpRoutes[Task] =
    createTodoRoute <+> getTodosRoute <+> getTodoRoute <+> updateTodoRoute <+> patchTodoRoute <+> deleteTodoRoute

  override def openApiRoutes(): HttpRoutes[Task] = {
    new SwaggerHttp4s(Api.openAPI).routes[Task]
  }

}
