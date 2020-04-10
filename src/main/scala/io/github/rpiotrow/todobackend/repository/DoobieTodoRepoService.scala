package io.github.rpiotrow.todobackend.repository

import cats.effect.Blocker
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.update.Update0
import doobie.{Query0, Transactor}
import io.github.rpiotrow.todobackend.domain.Todo
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext

class DoobieTodoRepoService(tnx: Transactor[Task]) extends TodoRepo.Service {
  import DoobieTodoRepoService._

  override def read(): Task[List[(Long, Todo)]] =
    SQL
      .readAll
      .stream
      .compile.toList
      .transact(tnx)
      .map(list => list.map(e => (e.id, toTodo(e))))
  override def read(id: Long): Task[Option[Todo]] =
    SQL
      .read(id)
      .option
      .transact(tnx)
      .map(option => option.map(toTodo))
  override def insert(e: Todo): Task[Long] =
    SQL
      .create(e)
      .withUniqueGeneratedKeys[Long]("id")
      .transact(tnx)
  override def update(id: Long, e: Todo): Task[Option[Unit]] =
    SQL
      .update(id, e)
      .run
      .transact(tnx)
      .map(oneToSomeUnit)
  override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]): Task[Option[Todo]] = {
    val update: ConnectionIO[Option[Unit]] = (maybeTitle, maybeCompleted) match {
      case (None, None) => AsyncConnectionIO.unit.map(_.some)
      case (Some(title), None) => SQL.update(id, title).run.map(oneToSomeUnit)
      case (None, Some(completed)) => SQL.update(id, completed).run.map(oneToSomeUnit)
      case (Some(title), Some(completed)) => SQL.update(id, title, completed).run.map(oneToSomeUnit)
    }
    (for {
      _ <- update
      e <- SQL.read(id).option
    } yield e)
      .transact(tnx)
      .map(option => option.map(toTodo))
  }
  override def delete(id: Long): Task[Option[Unit]] =
    SQL
      .delete(id)
      .run
      .transact(tnx)
      .map(oneToSomeUnit)

  private def oneToSomeUnit(r: Int) = if (r == 1) ().some else None
  private def toTodo(entity: TodoEntity) = Todo(entity.title, entity.completed)
}

object DoobieTodoRepoService {

  case class TodoEntity(
    id: Long,
    title: String,
    completed: Boolean
  )
  final case class TodoEntityNotFound(id: Long) extends Exception

  object SQL {

    def readAll(): Query0[TodoEntity] =
      sql"""SELECT * FROM todos""".query[TodoEntity]

    def read(id: Long): Query0[TodoEntity] =
      sql"""SELECT * FROM todos WHERE id = $id """.query[TodoEntity]

    def create(todo: Todo): Update0 =
      sql"""INSERT INTO todos (title, completed) VALUES (${todo.title}, ${todo.completed})""".update

    def update(id: Long, todo: Todo): Update0 =
      sql"""UPDATE todos SET title=${todo.title}, completed=${todo.completed} WHERE id = $id""".update

    def update(id: Long, title: String): Update0 =
      sql"""UPDATE todos SET title=$title WHERE id = $id""".update

    def update(id: Long, completed: Boolean): Update0 =
      sql"""UPDATE todos SET completed=$completed WHERE id = $id""".update

    def update(id: Long, title: String, completed: Boolean): Update0 =
      sql"""UPDATE todos SET title=$title, completed=$completed WHERE id = $id""".update

    def delete(id: Long): Update0 =
      sql"""DELETE FROM todos WHERE id = $id""".update
  }

  def mkTransactor(connectEC: ExecutionContext, transactEC: ExecutionContext): Managed[Throwable, DoobieTodoRepoService] = {
    import zio.interop.catz._

    val xa = HikariTransactor.newHikariTransactor[Task](
      "org.postgresql.Driver",
      "jdbc:postgresql:todobackend",
      "todobackend",
      "todobackend",
      connectEC,
      Blocker.liftExecutionContext(transactEC)
    )

    val res = xa.allocated.map {
      case (transactor, cleanupM) =>
        Reservation(ZIO.succeed(transactor), _ => cleanupM.orDie)
    }.uninterruptible

    Managed(res)
      .map(new DoobieTodoRepoService(_))
  }

}
