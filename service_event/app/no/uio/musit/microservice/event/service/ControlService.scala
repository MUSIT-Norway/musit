package no.uio.musit.microservice.event.service

// import no.uio.musit.microservice.event.domain.{ Control, ControlTemperature }
import no.uio.musit.microservice.event.domain.{ BaseEventProps, Event, SingleTableSingleDto }
import no.uio.musit.microservices.common.extensions.FutureExtensions._

class Control(baseProps: BaseEventProps) extends Event(baseProps)

object ControlService extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Control(baseEventProps)
  }
}

/*

object ControlTemperatureService extends SimpleService {

  override def fromDatabase(id: Long, base: EventBaseDto) =
    MusitFuture.successful(ControlTemperature(Some(id), base.links, base.note, None, base.valueLongToBool, None))

}






trait ControlSpesific extends EventFields {
  val ok: Boolean
  val observation: Option[ObservationSpesific] //only relevant for insert not select. Gets translated into motivatedBy-relation
}

case class Control(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

case class ControlTemperature(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]],
    ok: Boolean,
    observation: Option[ObservationSpesific]
) extends Event with ControlSpesific {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

*/ 