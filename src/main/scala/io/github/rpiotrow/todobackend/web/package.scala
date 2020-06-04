package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.web.Api._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

package object web {

  object ApiImplementation {
    trait Service[F[_]] {
      def getTodos: ServerEndpoint[Unit, ApiError, List[TodoOutput], Nothing, F]
      val getTodo: ServerEndpoint[Long, ApiError, TodoOutput, Nothing, F]
      val createTodo: ServerEndpoint[CreateTodoInput, ApiError, String, Nothing, F]
      val updateTodo: ServerEndpoint[(Long, UpdateTodoInput), ApiError,TodoOutput, Nothing, F]
      val patchTodo: ServerEndpoint[(Long, PatchTodoInput), ApiError, TodoOutput, Nothing, F]
      val deleteTodo: ServerEndpoint[Long, ApiError, StatusCode, Nothing, F]
    }
  }

}
