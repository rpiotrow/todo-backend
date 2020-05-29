package io.github.rpiotrow.todobackend.web.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import zio._

class AkkaHttpServerService(
  routes: Routes.Service,
  configuration: WebConfiguration
) extends Server.Service {

  override def server(): Task[Nothing] = {
    val managed = for {
      actorSystem <- actorSystem()
      binding <- serverBinding(actorSystem)
    } yield binding
    managed.useForever
  }

  private def actorSystem(): Managed[Throwable, ActorSystem] = {
    val acquire = ZIO.effect(ActorSystem("todo-backend-akka-http"))
    val release = (actorSystem: ActorSystem) => ZIO.effect(actorSystem.terminate()).orDie
    Managed.make(acquire)(release)
  }

  private def serverBinding(implicit actorSystem: ActorSystem): Managed[Throwable, ServerBinding] = {
    val acquire = ZIO.fromFuture(ec =>
      Http()(actorSystem).bindAndHandle(routes.todoRoutes(ec), configuration.host, configuration.port)
    )
    val release =
      (serverBinding: ServerBinding) => ZIO.fromFuture(implicit ec => serverBinding.unbind() ).orDie
    Managed.make(acquire)(release)
  }
}
