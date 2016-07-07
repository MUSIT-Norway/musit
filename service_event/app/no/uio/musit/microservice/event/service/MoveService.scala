package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{ BaseEventProps, Event }

class Move(baseProps: BaseEventProps) extends Event(baseProps)

object Move extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Move(baseEventProps)
  }
}
