package io.github.rpiotrow.todobackend

import io.github.rpiotrow.todobackend.domain.Todo
import zio._
import zio.stm._

package object repository {

  type TodoRepo = Has[TodoRepo.Service]

  object TodoRepo {
    trait Service {
      def read(): Task[List[(Long, Todo)]]
      def read(id: Long): Task[Option[Todo]]
      def insert(e: Todo): Task[Long]
      def update(id: Long, e: Todo): Task[Unit]
      def delete(id: Long): Task[Unit]
    }

    val live: Layer[Nothing, TodoRepo] = ZLayer.fromFunctionM(_ => (for {
      tMap <- TMap.empty[Long, Todo]
      tIdGenerator <- TRef.make(1L)
    } yield new InMemoryTodoRepoService(tMap, tIdGenerator)).commit)
  }

}
