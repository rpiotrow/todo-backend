package io.github.rpiotrow.todobackend.web

import zio.Has

package object http4s {

  type Routes = Has[Routes.Service]

  type Server = Has[Server.Service]

}
