package io.github.rpiotrow.todobackend.repository

import io.github.rpiotrow.todobackend.domain.Todo
import zio.{IO, Layer, UIO, ZLayer}

object TodoRepo {

  trait Service {
    def getAll(): IO[Throwable, List[Todo]]
    def get(id: Long): IO[Throwable, Option[Todo]]
  }

  object Service {
    val live = new TodoRepo.Service {
      private val dummyValue = Todo(
        id = 1L,
        title = "dummy",
        completed = false
      )
      def getAll(): IO[Throwable, List[Todo]] = UIO.succeed(List(dummyValue))
      def get(id: Long): IO[Throwable, Option[Todo]] = UIO.succeed(
        if (id == 1L) Some(dummyValue) else None
      )
    }
  }

  val live: Layer[Nothing, TodoRepo] =
    ZLayer.succeed(Service.live)

}
