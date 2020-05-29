package io.github.rpiotrow.todobackend.web

import cats.implicits._
import cats._
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import io.github.rpiotrow.todobackend.domain.Todo
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Api.TodoOutput
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import zio._

class ApiImplementationService[F[_]](
  repo: TodoRepo.Service,
  configuration: WebConfiguration,
  transformation: Task ~> F
) extends ApiImplementation.Service[F] {

  override val getTodos =
    addLogic(Api.getTodos) { _ =>
      for {
        list <- repo.read()
      } yield list.map { case (id, todo) => output(todo, id) }
    }

  override val getTodo =
    addLogic(Api.getTodo) { id =>
      repo
        .read(id).map {
        case Some(todo) => (output(todo, id).some, StatusCode.Ok)
        case None       => (None, StatusCode.NotFound)
      }
    }

  override val createTodo =
    addLogic(Api.createTodo){ input =>
      val todo = Todo(
        title = input.title,
        completed = false
      )
      repo.insert(todo).map(todoUrl)
    }

  override val updateTodo =
    addLogic(Api.updateTodo) { tuple =>
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

  override val patchTodo =
    addLogic(Api.patchTodo) { case (id, input) =>
      repo
        .update(id, input.title, input.completed).map {
        case Some(todo) => (output(todo, id).some, StatusCode.Ok)
        case None       => (None, StatusCode.NotFound)
      }
    }

  override val deleteTodo =
    addLogic(Api.deleteTodo) { id =>
      repo
        .delete(id).map {
        case Some(_) => StatusCode.NoContent
        case None    => StatusCode.NotFound
      }
    }

  private def addLogic[I, O](e: Endpoint[I, String, O, Nothing])(logic: I => Task[O]): ServerEndpoint[I, String, O, Nothing, F] = {
    e.serverLogic[F](input => {
      val task = logic(input).mapError(_.getMessage()).either
      transformation(task)
    })
  }

  private def todoUrl(id: Long) = s"http://${configuration.host}:${configuration.port}/todos/${id}"
  private def output(todo: Todo, id: Long) = TodoOutput(
    title = todo.title,
    url = todoUrl(id),
    completed = todo.completed
  )

}
