package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{ BaseEventProps, Event, SingleTableSingleDto }

class Observation(baseProps: BaseEventProps) extends Event(baseProps) {
  def subObservations = this.getSubEvents //TEMP!

}

object ObservationService extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Observation(baseEventProps)
  }
}

