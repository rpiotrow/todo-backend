package io.github.rpiotrow.todobackend.web

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml._

object Api {

  case class CreateTodoInput(title: String)
  case class UpdateTodoInput(title: String, completed: Boolean)
  case class PatchTodoInput(title: Option[String], completed: Option[Boolean])
  case class TodoOutput(title: String, url: String, completed: Boolean)  // TODO: use java.net.URL instead of String

  sealed trait ApiError
  case object NotFound extends ApiError
  case class ServerError(what: String) extends ApiError

  private val baseEndpoint: Endpoint[Unit, ApiError, Unit, Nothing] = endpoint.in("todos")
    .errorOut(
      oneOf[ApiError](
        statusMapping(StatusCode.NotFound, emptyOutput.map(_ => NotFound)(_ => ())),
        statusMapping(StatusCode.InternalServerError, jsonBody[ServerError].description("server error"))
      )
    )

  val getTodos: Endpoint[Unit, ApiError, List[TodoOutput], Nothing] = baseEndpoint
    .get
    .out(jsonBody[List[TodoOutput]])

  val getTodo: Endpoint[Long, ApiError, TodoOutput, Nothing] = baseEndpoint
    .get
    .in(path[Long]("id"))
    .out(jsonBody[TodoOutput])

  val createTodo: Endpoint[CreateTodoInput, ApiError, String, Nothing] = baseEndpoint
    .post
    .in(jsonBody[CreateTodoInput])
    .out(header[String]("location")) // TODO: use java.net.URL instead of String
    .out(statusCode(StatusCode.Created))

  val updateTodo: Endpoint[(Long, UpdateTodoInput), ApiError, TodoOutput, Nothing] = baseEndpoint
    .put
    .in(path[Long]("id"))
    .in(jsonBody[UpdateTodoInput])
    .out(jsonBody[TodoOutput])

  val patchTodo: Endpoint[(Long, PatchTodoInput), ApiError,TodoOutput, Nothing] = baseEndpoint
    .patch
    .in(path[Long]("id"))
    .in(jsonBody[PatchTodoInput])
    .out(jsonBody[TodoOutput])

  val deleteTodo: Endpoint[Long, ApiError, StatusCode, Nothing] = baseEndpoint
    .delete
    .in(path[Long]("id"))
    .out(statusCode)

  val openAPI: String = List(getTodos, getTodo, createTodo, updateTodo, patchTodo, deleteTodo)
    .toOpenAPI("Todo List", "1.0")
    .toYaml

}
