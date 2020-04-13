package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.configuration.Configuration
import io.github.rpiotrow.todobackend.domain.Todo
import zio._
import zio.blocking.Blocking
import zio.stm._

import scala.concurrent.ExecutionContext

package object repository {

  type TodoRepo = Has[TodoRepo.Service]

  object TodoRepo {
    trait Service {
      def read(): Task[List[(Long, Todo)]]
      def read(id: Long): Task[Option[Todo]]
      def insert(e: Todo): Task[Long]
      def update(id: Long, e: Todo): Task[Option[Unit]]
      def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]): Task[Option[Todo]]
      def delete(id: Long): Task[Option[Unit]]
    }

    val live: Layer[Throwable, TodoRepo] = ZLayer.fromFunctionM(_ => (for {
      tMap <- TMap.empty[Long, Todo]
      tIdGenerator <- TRef.make(1L)
    } yield new InMemoryTodoRepoService(tMap, tIdGenerator)).commit)

    def postgreSQL(connectEC: ExecutionContext): ZLayer[Blocking with Configuration, Throwable, TodoRepo] =
      ZLayer.fromManaged (
        for {
          blockingEC <- blocking.blocking { ZIO.descriptor.map(_.executor.asEC) }.toManaged_
          configuration <- configuration.databaseConfiguration.toManaged_
          managed <- DoobieTodoRepoService.mkTransactor(configuration, connectEC, blockingEC)
        } yield managed
      )

  }

}
