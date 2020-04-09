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

  private val baseEndpoint = endpoint.in("todos").errorOut(stringBody)

  val getTodos: Endpoint[Unit, String, List[TodoOutput], Nothing] = baseEndpoint
    .get
    .out(jsonBody[List[TodoOutput]])

  val getTodo: Endpoint[Long, String, (Option[TodoOutput], StatusCode), Nothing] = baseEndpoint
    .get
    .in(path[Long]("id"))
    .out(jsonBody[Option[TodoOutput]])
    .out(statusCode)

  val createTodo: Endpoint[CreateTodoInput, String, String, Nothing] = baseEndpoint
    .post
    .in(jsonBody[CreateTodoInput])
    .out(header[String]("location")) // TODO: use java.net.URL instead of String
    .out(statusCode(StatusCode.Created))

  val updateTodo: Endpoint[(Long, UpdateTodoInput), String, (Option[TodoOutput], StatusCode), Nothing] = baseEndpoint
    .put
    .in(path[Long]("id"))
    .in(jsonBody[UpdateTodoInput])
    .out(jsonBody[Option[TodoOutput]])
    .out(statusCode)

  val patchTodo: Endpoint[(Long, PatchTodoInput), String, (Option[TodoOutput], StatusCode), Nothing] = baseEndpoint
    .patch
    .in(path[Long]("id"))
    .in(jsonBody[PatchTodoInput])
    .out(jsonBody[Option[TodoOutput]])
    .out(statusCode)

  val deleteTodo: Endpoint[Long, String, StatusCode, Nothing] = baseEndpoint
    .delete
    .in(path[Long]("id"))
    .out(statusCode)

  val openAPI: String = List(getTodos, getTodo, createTodo, updateTodo, patchTodo, deleteTodo)
    .toOpenAPI("Todo List", "1.0")
    .toYaml

}
