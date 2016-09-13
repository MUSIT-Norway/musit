package no.uio.musit.microservice.event.service

// import no.uio.musit.microservice.event.domain.{ Control, ControlTemperature }
import no.uio.musit.microservice.event.domain.{ BaseEventDto, Event }
import no.uio.musit.microservices.common.extensions.FutureExtensions._

class Control(baseProps: BaseEventDto) extends Event(baseProps)

object ControlService extends SingleTableNotUsingCustomFields {

  //override def storeObjectsInPlaceRelationTable = true // place is viewed as object in this event.

  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new Control(baseEventProps)
  }
}
