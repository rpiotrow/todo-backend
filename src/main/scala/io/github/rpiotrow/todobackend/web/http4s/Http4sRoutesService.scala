package io.github.rpiotrow.todobackend.web.http4s

import cats.arrow.FunctionK
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.{Api, ApiImplementationService}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._

class Http4sRoutesService(
  repo: TodoRepo.Service,
  configuration: WebConfiguration
) extends Routes.Service {

  override def todoRoutes(): HttpRoutes[Task] = {
    val apiImplementation = new ApiImplementationService(repo, configuration, FunctionK.id)
    List(
      apiImplementation.getTodo,
      apiImplementation.getTodos,
      apiImplementation.createTodo,
      apiImplementation.updateTodo,
      apiImplementation.patchTodo,
      apiImplementation.deleteTodo
    ).toRoutes
  }

  override def openApiRoutes(): HttpRoutes[Task] = {
    new SwaggerHttp4s(Api.openAPI).routes[Task]
  }

}
