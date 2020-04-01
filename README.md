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
 * [cats](https://typelevel.org/cats/) as utility library (and dependency of http4s)
 * [tapir](https://tapir.softwaremill.com/) to describe endpoint (API)
 * [logback](http://logback.qos.ch/) for logging
 * [spec2](https://etorreborre.github.io/specs2/) as test framework
