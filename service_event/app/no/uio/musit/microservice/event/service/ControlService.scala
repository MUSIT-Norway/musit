package no.uio.musit.microservice.event.service

// import no.uio.musit.microservice.event.domain.{ Control, ControlTemperature }
import no.uio.musit.microservice.event.domain.{ BaseEventProps, Event }
import no.uio.musit.microservices.common.extensions.FutureExtensions._

class Control(baseProps: BaseEventProps) extends Event(baseProps)

object ControlService extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Control(baseEventProps)
  }
}
