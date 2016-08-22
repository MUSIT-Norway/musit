package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{ BaseEventDto, Event }

class MoveObject(baseProps: BaseEventDto) extends Event(baseProps) {
  def execute(): Unit = {

  }

}

object MoveObject extends SingleTableNotUsingCustomFields {

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new MoveObject(baseEventProps)
  }
}

class MovePlace(baseProps: BaseEventDto) extends Event(baseProps)

object MovePlace extends SingleTableNotUsingCustomFields {

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new MovePlace(baseEventProps)
  }
}