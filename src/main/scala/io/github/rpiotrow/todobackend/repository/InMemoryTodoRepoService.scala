package io.github.rpiotrow.todobackend.repository

import cats.implicits._
import io.github.rpiotrow.todobackend.domain.Todo
import zio.stm._

class InMemoryTodoRepoService(
  private val tMap: TMap[Long, Todo],
  private val tIdGenerator: TRef[Long]
) extends TodoRepo.Service {

  override def read() =
    tMap.toList.commit
  override def read(id: Long) =
    tMap.get(id).commit
  override def insert(e: Todo) = (for {
    id <- tIdGenerator.getAndUpdate(_ + 1)
    _  <- tMap.put(id, e)
  } yield id).commit
  override def update(id: Long, e: Todo) = (for {
    contains <- tMap.contains(id)
    result   =  if (contains) ().some else None
    _        <- tMap.put(id, e)
  } yield result).commit
  override def update(id: Long, maybeTitle: Option[String], maybeCompleted: Option[Boolean]) = (for {
    maybe   <- tMap.get(id)
    updated =  update(maybe, maybeTitle, maybeCompleted)
    _       <- updated.fold(STM.unit)(tMap.put(id, _))
  } yield updated).commit
  private def update(maybeTodo: Option[Todo], maybeTitle: Option[String], maybeCompleted: Option[Boolean]) = for {
    todo <- maybeTodo
    withTitle = maybeTitle.fold(todo)(title => todo.copy(title = title))
    withTitleAndCompleted  = maybeCompleted.fold(withTitle)(completed => withTitle.copy(completed = completed))
  } yield withTitleAndCompleted
  override def delete(id: Long) = (for {
    contains <- tMap.contains(id)
    result   =  if (contains) ().some else None
    -        <- tMap.delete(id)
  } yield result).commit

}
