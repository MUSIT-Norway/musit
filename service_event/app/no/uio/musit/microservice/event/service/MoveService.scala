package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{ BaseEventDto, Event }

class Move(baseProps: BaseEventDto) extends Event(baseProps)

object Move extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new Move(baseEventProps)
  }
}
