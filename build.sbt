val Http4sVersion = "0.21.2"
val TapirVersion = "0.12.24"
val CirceVersion = "0.13.0"
val Specs2Version = "4.8.3"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "io.github.rpiotrow",
    name := "todo-backend",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,

      "com.softwaremill.sttp.tapir" %% "tapir-core"               % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % TapirVersion,

      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,

      "org.specs2"      %% "specs2-core"         % Specs2Version % "test"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
)
