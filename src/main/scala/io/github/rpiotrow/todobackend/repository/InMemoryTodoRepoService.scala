package io.github.rpiotrow.todobackend.repository

import io.github.rpiotrow.todobackend.domain.Todo
import zio.stm._

class InMemoryTodoRepoService(
  private val tMap: TMap[Long, Todo],
  private val tIdGenerator: TRef[Long]
) extends TodoRepo.Service {

  def read() = tMap.toList.commit
  def read(id: Long) = tMap.get(id).commit
  def insert(e: Todo) = (for {
    id <- tIdGenerator.getAndUpdate(_ + 1)
    _  <- tMap.put(id, e)
  } yield id).commit
  def update(id: Long, e: Todo) = tMap.put(id, e).commit
  def delete(id: Long) = tMap.delete(id).commit

}
