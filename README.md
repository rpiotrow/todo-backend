# TODO application

Backend for TODO list. Goal of this application is testing technology stack.

## Requirements

 * Java SDK: https://jdk.java.net/13/
 * sbt: https://www.scala-sbt.org/download.html

## Run

```
sbt run
```

## API

Go to `http://localhost:8080/docs` to access simple API documentation with Swagger UI.

## Libraries

 * [http4s](https://http4s.org/) as http server and client
 * [circe](https://circe.github.io/circe/) for JSON serialization
 * [ZIO](https://zio.dev/) as utility library
 * [tapir](https://tapir.softwaremill.com/) to describe endpoint (API)
 * [doobie](https://tpolecat.github.io/doobie/) to access SQL database
 * [PureConfig](https://pureconfig.github.io) to parse configuration file into case class
 * [logback](http://logback.qos.ch/) for logging
 * [spec2](https://etorreborre.github.io/specs2/) as test framework
