package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.{MoveObjectDao, MovePlaceDao}
import no.uio.musit.microservice.event.domain.{BaseEventDto, Event}
import slick.dbio.DBIO

class MoveObject(baseProps: BaseEventDto) extends Event(baseProps) {
  def doExecute(eventId: Long): DBIO[Unit] = {
    MoveObjectDao.executeMove(eventId, this)
  }
  override def execute = Some(doExecute)
}

object MoveObject extends SingleTableNotUsingCustomFields {
  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new MoveObject(baseEventProps)
  }
}

class MovePlace(baseProps: BaseEventDto) extends Event(baseProps) {
  def doExecute(eventId: Long): DBIO[Unit] = {
    MovePlaceDao.executeMove(eventId, this)
  }
  override def execute = Some(doExecute)

}

object MovePlace extends SingleTableNotUsingCustomFields {
  override def storeObjectsInPlaceRelationTable = true

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new MovePlace(baseEventProps)
  }
}