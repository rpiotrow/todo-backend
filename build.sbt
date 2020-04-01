
lazy val root = (project in file("."))
  .settings(
    organization := "io.github.rpiotrow",
    name := "todo-backend",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Versions.http4s,
      "org.http4s"      %% "http4s-circe"        % Versions.http4s,
      "org.http4s"      %% "http4s-dsl"          % Versions.http4s,
      "io.circe"        %% "circe-generic"       % Versions.circe,

      "dev.zio"         %% "zio"                 % Versions.zio,
      "dev.zio"         %% "zio-interop-cats"    % Versions.zioInteropCats,

      "com.softwaremill.sttp.tapir" %% "tapir-core"               % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % Versions.tapir,

      "ch.qos.logback"  %  "logback-classic"     % Versions.logback,

      "org.specs2"      %% "specs2-core"         % Versions.specs2 % "test"
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
