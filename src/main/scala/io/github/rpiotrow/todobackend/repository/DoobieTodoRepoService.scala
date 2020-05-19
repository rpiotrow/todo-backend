package io.github.rpiotrow.todobackend.repository

import cats.effect.Blocker
import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
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

  override def read(): Task[List[(Long, Todo)]] = {
    stream(todos)
      .compile.toList
      .transact(tnx)
      .map(list => list.map(e => (e.id, toTodo(e))))
  }

  override def read(id: Long): Task[Option[Todo]] = {
    readEntity(id)
      .map(_.map(toTodo))
      .transact(tnx)
  }

  override def insert(todo: Todo): Task[Long] = {
    run(quote { todos.insert(lift(toEntity(0, todo))).returningGenerated(_.id) })
      .transact(tnx)
  }

  override def update(id: Long, todo: Todo): Task[Option[Unit]] = {
    updateEntity(id, toEntity(id, todo))
      .transact(tnx)
      .map(oneToSomeUnit)
  }

  override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]): Task[Option[Todo]] = {
    val updateFields = updateTitle(maybeTitle) andThen updateCompleted(maybeCompleted)
    (for {
      read    <- readEntity(id)
      updated  = read.map(updateFields(_))
      _       <- updated.map(updateEntity(id, _)).sequence
    } yield updated.map(toTodo))
      .transact(tnx)
  }

  override def delete(id: Long): Task[Option[Unit]] = {
    run(quote { todoById(id).delete })
      .transact(tnx)
      .map(oneToSomeUnit)
  }

  private def readEntity(id: Long) =
    run(quote { todoById(id) }).map(_.headOption)
  private def updateEntity(id: Long, e: TodoEntity) =
    run(quote { todoById(id).update(lift(e)) })

  private def oneToSomeUnit(r: Long) = if (r == 1) ().some else None
  private def toTodo(entity: TodoEntity) = Todo(entity.title, entity.completed)
  private def toEntity(id: Long, todo: Todo) = TodoEntity(id, todo.title, todo.completed)

  private def updateTitle(maybeTitle: Option[String]) = (e: TodoEntity) =>
    maybeTitle.fold(e)(title => e.copy(title = title))
  private def updateCompleted(maybeCompleted: Option[Boolean]) = (e: TodoEntity) =>
    maybeCompleted.fold(e)(completed => e.copy(completed = completed))
}

object DoobieTodoRepoService {

  case class TodoEntity(
    id: Long,
    title: String,
    completed: Boolean
  )

  def createWithZIO(
    configuration: DatabaseConfiguration,
    connectEC: ExecutionContext,
    transactEC: ExecutionContext
  ): ZManaged[Any, Throwable, DoobieTodoRepoService] = {
    for {
      _ <- ZIO.effect(Class.forName(configuration.jdbcDriver)).toManaged_
      t <- initialTransactor(connectEC, Blocker.liftExecutionContext(transactEC))
      _ <- configureTransactor(t, configuration).toManaged_
    } yield new DoobieTodoRepoService(t)
  }

  private def initialTransactor(connectEC: ExecutionContext, blocker: Blocker): ZManaged[Any, Throwable, HikariTransactor[Task]] = {
    Managed.make(ZIO.effect(new HikariDataSource))(ds => ZIO.effect(ds.close()).orDie)
      .map(Transactor.fromDataSource[Task](_, connectEC, blocker))
  }

  private def configureTransactor(t: HikariTransactor[Task], configuration: DatabaseConfiguration): Task[Unit] = {
    t.configure { ds =>
      Task.succeed {
        ds setJdbcUrl configuration.jdbcUrl
        ds setUsername configuration.dbUsername
        ds setPassword configuration.dbPassword
      }
    }
  }

  def createWithCats(
      configuration: DatabaseConfiguration,
      connectEC: ExecutionContext,
      transactEC: ExecutionContext
    ): Managed[Throwable, DoobieTodoRepoService] = {
    import zio.interop.catz._

    val xa = HikariTransactor.newHikariTransactor[Task](
      configuration.jdbcDriver,
      configuration.jdbcUrl,
      configuration.dbUsername,
      configuration.dbPassword,
      connectEC,
      Blocker.liftExecutionContext(transactEC)
    )

    Managed.makeReserve(
      xa.allocated.map {
        case (transactor, cleanupM) =>
          Reservation(ZIO.succeed(transactor), _ => cleanupM.orDie)
      }.uninterruptible
    ).map(new DoobieTodoRepoService(_))
  }

}
