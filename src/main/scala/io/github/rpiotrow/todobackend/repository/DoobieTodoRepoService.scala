package io.github.rpiotrow.todobackend.repository

import cats.effect.Blocker
import cats.implicits._
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import io.github.rpiotrow.todobackend.configuration.DatabaseConfiguration
import io.github.rpiotrow.todobackend.domain.Todo
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext

class DoobieTodoRepoService(tnx: Transactor[Task]) extends TodoRepo.Service {
  import DoobieTodoRepoService.TodoEntity

  private val dc = new DoobieContext.Postgres(Literal)
  import dc._

  private val todos = quote { querySchema[TodoEntity]("todos") }
  private def todoById(id: Long) = quote { todos.filter(_.id == lift(id)) }

  override def read() = {
    stream(todos)
      .compile.toList
      .transact(tnx)
      .bimap(TodoRepoError, _.map(e => (e.id, toTodo(e))))
  }

  override def read(id: Long): IO[TodoRepoFailure, Todo] = fromOption(
    readEntity(id)
      .map(_.map(toTodo))
      .transact(tnx)
      .mapError(TodoRepoError)
  )

  override def insert(todo: Todo) = {
    run(quote { todos.insert(lift(toEntity(0, todo))).returningGenerated(_.id) })
      .transact(tnx)
      .mapError(TodoRepoError)
  }

  override def update(id: Long, todo: Todo) =
    updateEntity(id, toEntity(id, todo))
      .transact(tnx)
      .mapError(TodoRepoError)
      .flatMap(unitOrNotFound)

  override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]) = fromOption {
    val updateFields = updateTitle(maybeTitle) andThen updateCompleted(maybeCompleted)
    (for {
      read    <- readEntity(id)
      updated =  read.map(updateFields(_))
      _       <- updated.map(updateEntity(id, _)).sequence
    } yield updated.map(toTodo))
      .transact(tnx)
      .mapError(TodoRepoError)
  }

  override def delete(id: Long) =
    run(quote { todoById(id).delete })
      .transact(tnx)
      .mapError(TodoRepoError)
      .flatMap(unitOrNotFound)

  private def readEntity(id: Long) =
    run(quote { todoById(id) }).map(_.headOption)
  private def updateEntity(id: Long, e: TodoEntity) =
    run(quote { todoById(id).update(lift(e)) })

  private def unitOrNotFound(r: Long) =
    if (r == 1) ZIO.succeed(()) else ZIO.fail(TodoNotFound)
  private def toTodo(entity: TodoEntity) = Todo(entity.title, entity.completed)
  private def toEntity(id: Long, todo: Todo) = TodoEntity(id, todo.title, todo.completed)

  private def updateTitle(maybeTitle: Option[String]) = (e: TodoEntity) =>
    maybeTitle.fold(e)(title => e.copy(title = title))
  private def updateCompleted(maybeCompleted: Option[Boolean]) = (e: TodoEntity) =>
    maybeCompleted.fold(e)(completed => e.copy(completed = completed))

  private def fromOption[A](option: IO[TodoRepoError, Option[A]]): IO[TodoRepoFailure, A] =
    option.flatMap(ZIO.fromOption(_).orElseFail(TodoNotFound))
}

object DoobieTodoRepoService {

  case class TodoEntity(
    id: Long,
    title: String,
    completed: Boolean
  )

  def create(
      configuration: DatabaseConfiguration,
      connectEC: ExecutionContext,
      transactEC: ExecutionContext
    ): Managed[Throwable, DoobieTodoRepoService] = {

    HikariTransactor
      .newHikariTransactor[Task](
        configuration.jdbcDriver,
        configuration.jdbcUrl,
        configuration.dbUsername,
        configuration.dbPassword,
        connectEC,
        Blocker.liftExecutionContext(transactEC)
      )
      .toManagedZIO
      .map(new DoobieTodoRepoService(_))
  }

}
