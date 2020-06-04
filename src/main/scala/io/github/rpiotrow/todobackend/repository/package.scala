package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.configuration.DatabaseConfiguration
import io.github.rpiotrow.todobackend.domain.Todo
import zio._
import zio.blocking.Blocking
import zio.config.Config
import zio.stm._

import scala.concurrent.ExecutionContext

package object repository {

  type TodoRepo = Has[TodoRepo.Service]
  type TodoRepoEnv = Blocking with Config[DatabaseConfiguration]

  sealed trait TodoRepoFailure
  case object TodoNotFound extends TodoRepoFailure
  case class TodoRepoError(ex: Throwable) extends TodoRepoFailure

  object TodoRepo {
    trait Service {
      def read(): IO[TodoRepoError, List[(Long, Todo)]]
      def read(id: Long): IO[TodoRepoFailure, Todo]
      def insert(e: Todo): IO[TodoRepoError, Long]
      def update(id: Long, e: Todo): IO[TodoRepoFailure, Unit]
      def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]): IO[TodoRepoFailure, Todo]
      def delete(id: Long): IO[TodoRepoFailure, Unit]
    }

    val live: Layer[Throwable, TodoRepo] = ZLayer.fromFunctionM(_ => (for {
      tMap <- TMap.empty[Long, Todo]
      tIdGenerator <- TRef.make(1L)
    } yield new InMemoryTodoRepoService(tMap, tIdGenerator)).commit)

    def postgreSQL(connectEC: ExecutionContext): ZLayer[TodoRepoEnv, Throwable, TodoRepo] =
      ZLayer.fromManaged (
        for {
          blockingEC <- blocking.blocking { ZIO.descriptor.map(_.executor.asEC) }.toManaged_
          configuration <- zio.config.config[DatabaseConfiguration].toManaged_
          managed <- DoobieTodoRepoService.create(configuration, connectEC, blockingEC)
        } yield managed
      )

  }

}
