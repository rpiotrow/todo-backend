package io.github.rpiotrow.todobackend.configuration

case class AppConfiguration(
  databaseConfiguration: DatabaseConfiguration,
  webConfiguration: WebConfiguration
)

case class DatabaseConfiguration(
  jdbcDriver: String,
  jdbcUrl: String,
  dbUsername: String,
  dbPassword: String
)

case class WebConfiguration(
  host: String,
  port: Int
)
