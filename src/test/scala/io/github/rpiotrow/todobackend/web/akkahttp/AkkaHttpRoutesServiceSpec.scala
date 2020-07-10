package io.github.rpiotrow.todobackend.web.akkahttp

import akka.http.scaladsl.model.StatusCodes
import org.specs2.mutable.Specification
import akka.http.scaladsl.testkit.Specs2RouteTest
import io.github.rpiotrow.todobackend.configuration.WebConfiguration
import io.github.rpiotrow.todobackend.domain.Todo
import io.github.rpiotrow.todobackend.repository.TodoRepo
import org.specs2.mock._
import zio.ZIO
import io.github.rpiotrow.todobackend.repository.TodoNotFound
import io.github.rpiotrow.todobackend.web.Api.{CreateTodoInput, PatchTodoInput, UpdateTodoInput}
import io.circe.generic.auto._
import io.circe.syntax._

class AkkaHttpRoutesServiceSpec extends Specification with Specs2RouteTest with Mockito {

  "list" can {
    "return empty list" in {
      val routes = prepare {
        _.read().returns(ZIO.succeed(List()))
      }

      Get("/todos") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "[]"
      }
    }
    "return non-empty list" in {
      val routes = prepare {
        _.read().returns(ZIO.succeed(
          List((1L, Todo("asd", false)), (2L, Todo("dsa", true))))
        )
      }

      Get("/todos") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """[
            |{"title":"asd","url":"http://localhost:9999/todos/1","completed":false},
            |{"title":"dsa","url":"http://localhost:9999/todos/2","completed":true}
            |]""".stripMargin.replaceAll("\n", "")
      }
    }
  }
  "read" should {
    "return 200 when todo exists" in {
      val routes = prepare {
        _.read(1L).returns(ZIO.succeed(Todo("asd", false)))
      }

      Get("/todos/1") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """{"title":"asd","url":"http://localhost:9999/todos/1","completed":false}"""
      }
    }
    "return 404 when todo not exists" in {
      val routes = prepare {
        _.read(1L).returns(ZIO.fail(TodoNotFound))
      }

      Get("/todos/1") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
  "create" should {
    "be successful" in {
      val routes = prepare {
        _.insert(Todo("newOne", false)).returns(ZIO.succeed(1L))
      }

      val input = CreateTodoInput("newOne")
      Post("/todos", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        header("Location").map(_.value) shouldEqual Some("http://localhost:9999/todos/1")
        responseAs[String] shouldEqual ""
      }
    }
  }
  "update" should {
    val input = UpdateTodoInput("updated", true)
    "return 200 when todo exists" in {
      val routes = prepare {
        _.update(1L, Todo("updated", true)).returns(ZIO.succeed(()))
      }

      Put("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """{"title":"updated","url":"http://localhost:9999/todos/1","completed":true}"""
      }
    }
    "return 404 when todo not exists" in {
      val routes = prepare {
        _.update(1L, Todo("updated", true)).returns(ZIO.fail(TodoNotFound))
      }

      Put("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
  "patch" can {
    "update title" in {
      val routes = prepare {
        _.update(1L, Some("updated"), None).returns(ZIO.succeed(Todo("updated", false)))
      }

      val input = PatchTodoInput(Some("updated"), None)
      Patch("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """{"title":"updated","url":"http://localhost:9999/todos/1","completed":false}"""
      }
    }
    "update completed" in {
      val routes = prepare {
        _.update(1L, None, Some(true)).returns(ZIO.succeed(Todo("old", true)))
      }

      val input = PatchTodoInput(None, Some(true))
      Patch("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """{"title":"old","url":"http://localhost:9999/todos/1","completed":true}"""
      }
    }
    "update title and completed" in {
      val routes = prepare {
        _.update(1L, Some("updated"), Some(true)).returns(ZIO.succeed(Todo("updated", true)))
      }

      val input = PatchTodoInput(Some("updated"), Some(true))
      Patch("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          """{"title":"updated","url":"http://localhost:9999/todos/1","completed":true}"""
      }
    }
    "return 404 when todo not exists" in {
      val routes = prepare {
        _.update(1L, Some("updated"), Some(true)).returns(ZIO.fail(TodoNotFound))
      }

      val input = PatchTodoInput(Some("updated"), Some(true))
      Patch("/todos/1", input.asJson.noSpaces) ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
  "delete" should {
    "return 204 when todo is deleted" in {
      val routes = prepare {
        _.delete(1L).returns(ZIO.succeed(()))
      }

      Delete("/todos/1") ~> routes ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }
    "return 404 when todo not exists" in {
      val routes = prepare {
        _.delete(1L).returns(ZIO.fail(TodoNotFound))
      }

      Delete("/todos/1") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  private def prepare(setup: TodoRepo.Service => Unit) = {
    val mockTodoRepoService = mock[TodoRepo.Service]
    setup(mockTodoRepoService)
    new AkkaHttpRoutesService(
      mockTodoRepoService,
      WebConfiguration("localhost", 9999)
    ).todoRoutes
  }
}
