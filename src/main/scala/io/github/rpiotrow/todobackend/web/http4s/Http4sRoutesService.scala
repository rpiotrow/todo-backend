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
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._

class Http4sRoutesService(repo: TodoRepo.Service, configuration: WebConfiguration) extends Routes.Service {

  private def http4sRoutes[I, O](e: Endpoint[I, String, O, EntityBody[Task]])(logic: I => Task[O]): HttpRoutes[Task] = {
    e.toRoutes { input => logic(input).mapError(_.getMessage()).either }
  }

  private def getTodosRoute: HttpRoutes[Task] = {
    http4sRoutes(getTodos) { _ =>
      repo.read().map(_.map { case (id, todo) => output(todo, id) })
    }
  }
  private def getTodoRoute: HttpRoutes[Task] = {
    http4sRoutes(getTodo) { id =>
      repo
        .read(id).map {
          case Some(todo) => (output(todo, id).some, StatusCode.Ok)
          case None       => (None, StatusCode.NotFound)
        }
    }
  }
  private def createTodoRoute: HttpRoutes[Task] = {
    http4sRoutes(createTodo) { input =>
      val todo = Todo(
        title = input.title,
        completed = false
      )
      repo.insert(todo).map(todoUrl)
    }
  }
  private def updateTodoRoute(): HttpRoutes[Task] = {
    http4sRoutes(updateTodo) { tuple =>
      val (id, input) = tuple
      val todo = Todo(
        title = input.title,
        completed = input.completed
      )
      repo
        .update(id, todo).map {
          case Some(_) => (output(todo, id).some, StatusCode.Ok)
          case None    => (None, StatusCode.NotFound)
        }
    }
  }
  private def patchTodoRoute(): HttpRoutes[Task] = {
    http4sRoutes(patchTodo) { case (id, input) =>
      repo
        .update(id, input.title, input.completed).map {
          case Some(todo) => (output(todo, id).some, StatusCode.Ok)
          case None       => (None, StatusCode.NotFound)
        }
    }
  }
  private def deleteTodoRoute(): HttpRoutes[Task] = {
    http4sRoutes(deleteTodo) { id =>
      repo
        .delete(id).map {
          case Some(_) => StatusCode.NoContent
          case None    => StatusCode.NotFound
        }
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
