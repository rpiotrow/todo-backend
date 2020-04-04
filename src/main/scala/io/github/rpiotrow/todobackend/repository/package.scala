package io.github.rpiotrow.todobackend

import zio.Has

package object repository {

  type TodoRepo = Has[TodoRepo.Service]

}
