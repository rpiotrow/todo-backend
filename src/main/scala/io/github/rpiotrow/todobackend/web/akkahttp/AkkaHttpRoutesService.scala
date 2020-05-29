package io.github.rpiotrow.todobackend.web.akkahttp

import akka.http.scaladsl.server.Route
import cats.arrow.FunctionK
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.{Api, ApiImplementationService}
import sttp.tapir.server.akkahttp._
import sttp.tapir.swagger.akkahttp.SwaggerAkka
import zio._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpRoutesService(
  repo: TodoRepo.Service,
  configuration: WebConfiguration
) extends Routes.Service {

  def toFuture(implicit ec: ExecutionContext) =
    λ[FunctionK[Task, Future]](task => Future(zio.Runtime.default.unsafeRunTask(task)))

  override def todoRoutes(implicit ec: ExecutionContext): Route = {
    val apiImplementation = new ApiImplementationService(repo, configuration, toFuture)
    List(
      apiImplementation.getTodo,
      apiImplementation.getTodos,
      apiImplementation.createTodo,
      apiImplementation.updateTodo,
      apiImplementation.patchTodo,
      apiImplementation.deleteTodo
    ).toRoute
  }

  override def openApiRoutes(): Route = new SwaggerAkka(Api.openAPI).routes

}
