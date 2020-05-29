package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.web.Api.{CreateTodoInput, PatchTodoInput, TodoOutput, UpdateTodoInput, baseEndpoint}
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import zio.{Has, Task}

package object web {

  object ApiImplementation {
    trait Service[F[_]] {
      def getTodos: ServerEndpoint[Unit, String, List[TodoOutput], Nothing, F]
      val getTodo: ServerEndpoint[Long, String, (Option[TodoOutput], StatusCode), Nothing, F]
      val createTodo: ServerEndpoint[CreateTodoInput, String, String, Nothing, F]
      val updateTodo: ServerEndpoint[(Long, UpdateTodoInput), String, (Option[TodoOutput], StatusCode), Nothing, F]
      val patchTodo: ServerEndpoint[(Long, PatchTodoInput), String, (Option[TodoOutput], StatusCode), Nothing, F]
      val deleteTodo: ServerEndpoint[Long, String, StatusCode, Nothing, F]
    }
  }

}
