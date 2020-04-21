package io.github.rpiotrow.todobackend.web.http4s

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.rpiotrow.todobackend.configuration.Configuration
import io.github.rpiotrow.todobackend.domain.Todo
import io.github.rpiotrow.todobackend.repository.TodoRepo
import io.github.rpiotrow.todobackend.web.Api.{CreateTodoInput, PatchTodoInput, TodoOutput, UpdateTodoInput}
import org.http4s.circe.CirceEntityCodec._
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock._
import zio.test.{DefaultRunnableSpec, suite, _}

object TodoRepoMock
{
  sealed trait Tag[I, A] extends Method[TodoRepo, I, A] {
    def envBuilder: URLayer[Has[Proxy], TodoRepo] =
      TodoRepoMock.envBuilder
  }

  object ReadAll extends Tag[Unit, List[(Long, Todo)]]
  object ReadOne extends Tag[Long, Option[Todo]]
  object Insert extends Tag[Todo, Long]
  object Update extends Tag[(Long, Todo), Option[Unit]]
  object UpdatePartially extends Tag[(Long, Option[String], Option[Boolean]), Option[Todo]]
  object Delete extends Tag[Long, Option[Unit]]

  private val envBuilder: URLayer[Has[Proxy], TodoRepo] =
    ZLayer.fromService(invoke =>
      new TodoRepo.Service {
        override def read(): Task[List[(Long, Todo)]] = invoke(ReadAll)
        override def read(id: Long): Task[Option[Todo]] = invoke(ReadOne, id)
        override def insert(e: Todo): Task[Long] = invoke(Insert, e)
        override def update(id: Long, e: Todo): Task[Option[Unit]] = invoke(Update, id, e)
        override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]): Task[Option[Todo]] = invoke(UpdatePartially, id, maybeTitle, maybeCompleted)
        override def delete(id: Long): Task[Option[Unit]] = invoke(Delete, id)
      }
    )
}

object Http4sRoutesServiceSpec extends DefaultRunnableSpec {
  import TodoRepoMock._
  import org.http4s._

  def spec = suite("Http4sRoutesServiceSpec")(
    testM("read all when empty") {
      val r = makeRequest(
        ReadAll returns value(List()),
        Request(method = Method.GET, uri = Uri(path = "/todos"))
      )
      check(r, Status.Ok, Some(List[TodoOutput]()))
    },
    testM("read all") {
      val r = makeRequest(
        ReadAll returns value(List((1, Todo("asd", false)), (2, Todo("dsa", true)))),
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
        ReadOne(equalTo(1L)) returns value(Some(Todo("asd", false))),
        Request(method = Method.GET, uri = Uri(path = "/todos/1"))
      )
      check(r, Status.Ok, Some(TodoOutput("asd", "http://localhost:8080/todos/1", false)))
    },
    testM("read one when not exists") {
      val r = makeRequest(
        ReadOne(equalTo(2L)) returns value(None),
        Request(method = Method.GET, uri = Uri(path = "/todos/2"))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("create") {
      val input = CreateTodoInput("newOne")
      val r = makeRequest(
        Insert(equalTo(Todo("newOne", false))) returns value(3L),
        Request(method = Method.POST, uri = Uri(path = "/todos"), body = httpEntity(input))
      )
      // assert Location header
      check[TodoOutput](r, Status.Created)
    },
    testM("update when exists") {
      val input = UpdateTodoInput("updated", true)
      val r = makeRequest(
        Update(equalTo((1L, Todo("updated", true)))) returns value(Some(())),
        Request(method = Method.PUT, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("update when does not exist") {
      val input = UpdateTodoInput("updated", true)
      val r = makeRequest(
        Update(equalTo((3L, Todo("updated", true)))) returns value(None),
        Request(method = Method.PUT, uri = Uri(path = "/todos/3"), body = httpEntity(input))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("patch title") {
      val input = PatchTodoInput(Some("updated"), None)
      val r = makeRequest(
        UpdatePartially(equalTo((1L, Some("updated"), None))) returns value(Some(Todo("updated", false))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", false)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch completed") {
      val input = PatchTodoInput(None, Some(true))
      val r = makeRequest(
        UpdatePartially(equalTo((1L, None, Some(true)))) returns value(Some(Todo("old", true))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("old", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch title and completed") {
      val input = PatchTodoInput(Some("updated"), Some(true))
      val r = makeRequest(
        UpdatePartially(equalTo((1L, Some("updated"), Some(true)))) returns value(Some(Todo("updated", true))),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/1"), body = httpEntity(input))
      )
      val expectedBody = TodoOutput("updated", "http://localhost:8080/todos/1", true)
      check[TodoOutput](r, Status.Ok, Some(expectedBody))
    },
    testM("patch when does not exist") {
      val input = PatchTodoInput(Some("updated"), Some(true))
      val r = makeRequest(
        UpdatePartially(equalTo((3L, Some("updated"), Some(true)))) returns value(None),
        Request(method = Method.PATCH, uri = Uri(path = "/todos/3"), body = httpEntity(input))
      )
      check[TodoOutput](r, Status.NotFound)
    },
    testM("delete when exists") {
      val r = makeRequest(
        Delete(equalTo(1L)) returns value(Some(())),
        Request(method = Method.DELETE, uri = Uri(path = "/todos/1"))
      )
      check[TodoOutput](r, Status.NoContent)
    },
    testM("delete when does not exist") {
      val r = makeRequest(
        Delete(equalTo(2L)) returns value(None),
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

    app.provideLayer(mockEnv ++ Configuration.live >>> Routes.live)
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
