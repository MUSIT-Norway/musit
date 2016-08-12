package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.Event
import no.uio.musit.microservice.event.dto.BaseEventDto

class Move(baseProps: BaseEventDto) extends Event(baseProps)

object Move extends SingleTableNotUsingCustomFields {

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new Move(baseEventProps)
  }
}
