package io.github.rpiotrow.todobackend.repository

import cats.effect.Blocker
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.rpiotrow.todobackend.domain.Todo
import org.scalatest.matchers._
import org.scalatest.wordspec.AnyWordSpec
import zio.Task
import zio.interop.catz._

class DoobieTodoRepoServiceSpec extends AnyWordSpec with ForAllTestContainer with should.Matchers {

  override val container = PostgreSQLContainer()

  private def prepareService() = {
    val xa = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver",
      container.jdbcUrl,
      container.username,
      container.password,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )
    val initialData = sql"""
                           |CREATE TABLE IF NOT EXISTS todos(
                           |    id SERIAL PRIMARY KEY,
                           |    title VARCHAR(255),
                           |    completed BOOLEAN
                           |);
                           |INSERT INTO todos(title, completed) VALUES ('test', false);
                           |""".stripMargin.update.run.transact(xa)
    zio.Runtime.default.unsafeRunTask(initialData)
    new DoobieTodoRepoService(xa)
  }

  private def insertFreshTodo(title: String, completed: Boolean) = {
    zio.Runtime.default.unsafeRunTask(service.insert(Todo(title, completed)).either) match {
      case Left(_) => fail()
      case Right(id) => id
    }
  }

  private lazy val service = prepareService()

  "TodoRepository" when {
    "read is invoked" should {
      "return list" in {
        val result = zio.Runtime.default.unsafeRunTask(service.read().either)
        result should be (Right(List((1, Todo("test", false)))))
      }
      "return existing todo" in {
        val result = zio.Runtime.default.unsafeRunTask(service.read(1L).either)
        result should be (Right(Todo("test", false)))
      }
      "not return non-existing todo" in {
        val result = zio.Runtime.default.unsafeRunTask(service.read(2L).either)
        result should be (Left(TodoNotFound))
      }
    }
    "insert is invoked" should {
      "persist new todo" in {
        val result = zio.Runtime.default.unsafeRunTask(service.insert(Todo("new", false)).either)
        result should be (Right(2L))
      }
    }
    "update is invoked" should {
      "update existing todo" in {
        val result = zio.Runtime.default.unsafeRunTask(service.update(1, Todo("updated", true)).either)
        result should be (Right(()))
      }
      "return error when todo is not found" in {
        val result = zio.Runtime.default.unsafeRunTask(service.update(10, Todo("updated", true)).either)
        result should be (Left(TodoNotFound))
      }
    }
    "partial update is invoked" should {
      "update nothing" in {
        val todoId = insertFreshTodo("old", false)
        val result = zio.Runtime.default.unsafeRunTask(service.update(todoId, None, None).either)
        result should be (Right(Todo("old", false)))
      }
      "update title only" in {
        val todoId = insertFreshTodo("old", false)
        val result = zio.Runtime.default.unsafeRunTask(service.update(todoId, Some("new"), None).either)
        result should be (Right(Todo("new", false)))
      }
      "update completed only" in {
        val todoId = insertFreshTodo("old", false)
        val result = zio.Runtime.default.unsafeRunTask(service.update(todoId, None, Some(true)).either)
        result should be (Right(Todo("old", true)))
      }
      "update title and completed" in {
        val todoId = insertFreshTodo("old", false)
        val result = zio.Runtime.default.unsafeRunTask(service.update(todoId, Some("new"), Some(true)).either)
        result should be (Right(Todo("new", true)))
      }
      "return error when todo is not found" in {
        val result = zio.Runtime.default.unsafeRunTask(service.update(101, None, None).either)
        result should be (Left(TodoNotFound))
      }
    }
    "delete is invoked" should {
      "delete existing todo" in {
        val todoId = insertFreshTodo("old", false)
        val result = zio.Runtime.default.unsafeRunTask(service.delete(todoId).either)
        result should be (Right(()))
      }
      "return error when todo not exists" in {
        val result = zio.Runtime.default.unsafeRunTask(service.update(101, None, None).either)
        result should be (Left(TodoNotFound))
      }
    }
  }
}
