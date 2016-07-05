package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{ BaseEventProps, Event, SingleTableSingleDto }

class Observation(baseProps: BaseEventProps) extends Event(baseProps)

object ObservationService extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Observation(baseEventProps)
  }
}

