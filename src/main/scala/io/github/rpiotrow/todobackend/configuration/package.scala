package io.github.rpiotrow.todobackend

import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import pureconfig.generic.ProductHint
import zio._

package object configuration {

  type Configuration = Has[DatabaseConfiguration] with Has[WebConfiguration]

  object Configuration {
    val live: Layer[Throwable, Configuration] = {
      import pureconfig.generic.auto._
      implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

      ZLayer.fromEffectMany(
        Task
          .effect(ConfigSource.default.loadOrThrow[AppConfiguration])
          .map(c => Has(c.databaseConfiguration) ++ Has(c.webConfiguration)))
    }
  }

  val databaseConfiguration: RIO[Has[DatabaseConfiguration], DatabaseConfiguration] = ZIO.access(_.get)

}
