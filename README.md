# TODO application

Backend for TODO list. Goal of this application is testing technology stack.

## Requirements

 * Java SDK: https://jdk.java.net/
 * sbt: https://www.scala-sbt.org/download.html

## Run

### Http4s

```
sbt "runMain io.github.rpiotrow.todobackend.MainHttp4s"
```

### Akka HTTP

```
sbt "runMain io.github.rpiotrow.todobackend.MainAkkaHttp"
```

## API

Go to `http://localhost:8080/docs` to access simple API documentation with Swagger UI.

## Libraries

 * [http4s](https://http4s.org/) as http server
 * [akka-http](https://doc.akka.io/docs/akka-http/current/index.html) as alternative http server
 * [circe](https://circe.github.io/circe/) for JSON serialization
 * [ZIO](https://zio.dev/) as utility library
 * [tapir](https://tapir.softwaremill.com/) to describe endpoint (API)
 * [doobie](https://tpolecat.github.io/doobie/) to access SQL database
 * [quill](https://getquill.io/) to write SQL queries in Scala
 * [zio-config](https://zio.github.io/zio-config/) to parse configuration file into case class
 * [logback](http://logback.qos.ch/) for logging
 * [zio-test](https://zio.dev/docs/usecases/usecases_testing) as test framework
