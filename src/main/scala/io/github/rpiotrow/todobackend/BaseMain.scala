package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.MainAkkaHttp.platform
import io.github.rpiotrow.todobackend.configuration.{AppConfiguration, Configuration, DatabaseConfiguration, WebConfiguration}
import io.github.rpiotrow.todobackend.repository.TodoRepo
import zio.{Layer, ZLayer}
import zio.blocking.Blocking
import zio.config.Config
import zio.config.syntax._

object BaseMain {
  val configuration: Layer[Throwable, Config[AppConfiguration]] =
    Configuration.live
  val databaseConfiguration: ZLayer[Any, Throwable, Config[DatabaseConfiguration]] =
    configuration.narrow(_.databaseConfiguration)
  val webConfiguration: ZLayer[Any, Throwable, Config[WebConfiguration]] =
    configuration.narrow(_.webConfiguration)
  val persistence: ZLayer[Any, Throwable, TodoRepo] =
    (databaseConfiguration ++ Blocking.live) >>> TodoRepo.postgreSQL(platform.executor.asEC)
}
