package io.github.rpiotrow.todobackend.web.http4s

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.rpiotrow.todobackend.configuration.Configuration
import io.github.rpiotrow.todobackend.domain.Todo
import io.github.rpiotrow.todobackend.repository._
import io.github.rpiotrow.todobackend.web.Api._
import org.http4s.circe.CirceEntityCodec._
import zio._
import zio.config.syntax._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock._
import zio.test.{DefaultRunnableSpec, suite, _}

//compiles in sbt, but not in IntelliJ
//@mockable[TodoRepo.Service]
//object TodoRepoMock

object TodoRepoMock extends Mock[TodoRepo] {
  object Read {
    object All        extends Effect[Unit, TodoRepoError, List[(Long, Todo)]]
    object One        extends Effect[Long, TodoRepoFailure, Todo]
  }
  object Insert       extends Effect[Todo, TodoRepoError, Long]
  object Update {
    object Fully      extends Effect[(Long, Todo), TodoRepoFailure, Unit]
    object Partially  extends Effect[(Long, Option[String], Option[Boolean]), TodoRepoFailure, Todo]
  }
  object Delete       extends Effect[Long, TodoRepoFailure, Unit]

  val compose: URLayer[Has[Proxy], TodoRepo] =
    ZLayer.fromService { proxy =>
      new TodoRepo.Service {
        override def read()                    = proxy(Read.All)
        override def read(id: Long)            = proxy(Read.One, id)
        override def insert(e: Todo)           = proxy(Insert, e)
        override def update(id: Long, e: Todo) = proxy(Update.Fully, (id, e))
        override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]) =
          proxy(Update.Partially, id, maybeTitle, maybeCompleted)
        override def delete(id: Long)          = proxy(Delete, id)
      }
    }}

object Http4sRoutesServiceSpec extends DefaultRunnableSpec {
  import TodoRepoMock._
  import org.http4s._

  def spec = suite("Http4sRoutesServiceSpec")(
    testM("read all when empty") {
      val r = makeRequest(
        Read.All(value(List())),
        Request(method = Method.GET, uri = Uri(path = "/todos"))
      )
      check(r, Status.Ok, Some(List[TodoOutput]()))
    },
    testM("read all") {
      val r = makeRequest(
        Read.All(value(List((1L, Todo("asd", false)), (2L, Todo("dsa", true))))),
        Request(method = Method.GET, uri = Uri(path = "/todos"))
      )
      val expectedBody = List(
        TodoOutput("asd", "http://localhost:8080/todos/1", false),
        TodoOutput("dsa", "http://localhost:8080/todos/2", true)
      )
      check(r, Status.Ok, Some(expectedBody))
    },
    testM("read one when exists") {
      val r = makeRequest(
        Read.One(equalTo(1L), value(Todo("asd", false))),
        Request(method = Method.GET, uri = Uri(path = "/todos/1"))
      )
      check(r, Status.Ok, Some(TodoOutput("asd", "http://localhost:8080/todos/1", false)))
    },
    testM("read one when not exists") {
      val r = makeRequest(
        Read.One(equalTo(2L), failure(TodoNotFound)),
        Request(method = Method.GET, uri = Uri(path = "/todos/2"))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("create") {
      val input = CreateTodoInput("newOne")
      val r = makeRequest(
        Insert(equalTo(Todo("newOne", false)), value(3L)),
        Request(method = Method.POST, uri = Uri(path = "/todos"), body = httpEntity(input))
      )
      // TODO: assert Location header
      check[TodoOutput](r, Status.Created)
    },
    testM("update when exists") {
      val input = UpdateTodoInput("updated", true)
      val r = makeRequest(
        Update.Fully(equalTo((1L, Todo("updated", true))), value(Some(()))),
        Request(method = Method.PUT, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("update when does not exist") {
      val input = UpdateTodoInput("updated", true)
      val r = makeRequest(
        Update.Fully(equalTo((3L, Todo("updated", true))), failure(TodoNotFound)),
        Request(method = Method.PUT, uri = Uri(path = "/todos/3"), body = httpEntity(input))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("patch title") {
      val input = PatchTodoInput(Some("updated"), None)
      val r = makeRequest(
        Update.Partially(equalTo((1L, Some("updated"), None)), value(Todo("updated", false))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", false)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch completed") {
      val input = PatchTodoInput(None, Some(true))
      val r = makeRequest(
        Update.Partially(equalTo((1L, None, Some(true))), value(Todo("old", true))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("old", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch title and completed") {
      val input = PatchTodoInput(Some("updated"), Some(true))
      val r = makeRequest(
        Update.Partially(equalTo((1L, Some("updated"), Some(true))), value(Todo("updated", true))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch when does not exist") {
      val input = PatchTodoInput(Some("updated"), Some(true))
      val r = makeRequest(
        Update.Partially(equalTo((3L, Some("updated"), Some(true))), failure(TodoNotFound)),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/3"), body = httpEntity(input))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("delete when exists") {
      val r = makeRequest(
        Delete(equalTo(1L), value(Some(()))),
        Request(method = Method.DELETE, uri = Uri(path = "/todos/1"))
      )
      check[TodoOutput](r, Status.NoContent)
    },
    testM("delete when does not exist") {
      val r = makeRequest(
        Delete(equalTo(2L), failure(TodoNotFound)),
        Request(method = Method.DELETE, uri = Uri(path = "/todos/2"))
      )
      check[TodoOutput](r, Status.NotFound)
    }
  )

  private def makeRequest(mockEnv: TaskLayer[TodoRepo], request: Request[Task]) = {
    val app = for {
      routes <- Routes.todoRoutes
      response <- routes.run(request).value
    } yield response.getOrElse(Response.notFound)

    val webConfiguration = Configuration.live.narrow(_.webConfiguration)
    app.provideLayer(mockEnv ++ webConfiguration >>> Routes.live)
  }

  private def httpEntity[A](a: A)(implicit ev: Encoder[A]) = {
    fs2.Stream.evalSeq(zio.Task { a.asJson.toString().getBytes.toSeq })
  }

  private def check[A](
    actual: Task[Response[Task]],
    expectedStatus: Status,
    expectedBody: Option[A] = None
  )(implicit ev: EntityDecoder[Task, A]) = {
    for {
      r <- actual
      statusAssert = assert(r.status)(equalTo(expectedStatus))
      bodyAssert <- expectedBody.fold(assertEmptyBody(r))(e => assertBody(r, e))
    } yield statusAssert && bodyAssert
  }

  private def assertEmptyBody(r: Response[Task]) = {
    for {
      body <- r.body.compile.toVector
    } yield assert(body)(isEmpty)
  }

  private def assertBody[A](r: Response[Task], expectedBody: A)(implicit ev: EntityDecoder[Task, A]) = {
    for {
      body <- r.as[A]
    } yield assert(body)(equalTo(expectedBody))
  }

}
