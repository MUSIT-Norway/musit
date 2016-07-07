package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{BaseEventDto, Event}

class Observation(baseProps: BaseEventDto) extends Event(baseProps) {
  val subObservations = this.getAllSubEventsAs[Observation] //TEMP!   //Can be subtyped to SpecificObservation when that has been created

}

object ObservationService extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new Observation(baseEventProps)
  }
}

