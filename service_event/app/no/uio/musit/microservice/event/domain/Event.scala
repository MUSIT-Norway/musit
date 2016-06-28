package no.uio.musit.microservice.event.domain

import julienrf.json.derived.flat
import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.dao.{ ControlDTO, ObservationDTO }
import no.uio.musit.microservice.event.service.{ ControlFactory, ObservationFactory }
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._

sealed trait Event {
  val eventType: EventType
  val id: Option[Long]
  val links: Option[Seq[Link]]
  val note: Option[String]
}

case class Move(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String]
) extends Event {
  val eventType = EventType.getByName(Move.getClass.getSimpleName)
}

case class Control(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    controlType: Option[String],
    controlOk: Boolean
) extends Event {
  val eventType = EventType.getByName(Control.getClass.getSimpleName)
}

object Control {
  def apply(base: EventBase, controlEvent: ControlDTO): Control =
    Control(
      base.id,
      base.links,
      base.note,
      controlEvent.controlType,
      base.valueLong.get > 2
    )
}

case class Observation(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    temperature: Option[Double]
) extends Event {
  val eventType = EventType.getByName(Observation.getClass.getSimpleName)
}

object Observation {
  def apply(base: EventBase, observation: ObservationDTO): Observation =
    Observation(
      base.id,
      base.links,
      base.note,
      observation.temperature
    )
}

object Event {
  implicit lazy val format: OFormat[Event] = flat.oformat((__ \ "type").format[String])
}
