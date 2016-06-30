package no.uio.musit.microservice.event.domain

import julienrf.json.derived.flat
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._

trait EventFields {
  val eventType: EventType
  val id: Option[Long]
  val links: Option[Seq[Link]]
  val note: Option[String]
}

trait EventWithSubEvents extends EventFields {
  val subEvents: Option[Seq[Event]]
}

sealed trait Event extends EventFields

object MoveO {
  sealed trait MoveOp extends EventFields {
    val eventType = EventType.getByName(this.getClass.getSimpleName)
  }
  case class Move(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[MoveOp]]
  ) extends MoveOp

  object MoveOp {
    implicit lazy val format: OFormat[MoveOp] = flat.oformat((__ \ "type").format[String])
  }
}

case class Move(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[MoveO.MoveOp]]
) extends Event with MoveO.MoveOp {
  //val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object Move {
  implicit val format = Json.format[Move]
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

sealed trait ObservationSpesific extends EventFields

case class ObservationTemperature(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String]
) extends ObservationSpesific {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object ObservationSpesific {
  implicit lazy val format: OFormat[ObservationSpesific] = flat.oformat((__ \ "type").format[String])
}

case class Observation(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]]
//temperature: Option[Double]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

/*object Observation {
  // FIXME should possibly reside in the service, since the service is the integrator between dto and public domain
  def apply(base: EventBaseDto, observation: ObservationDTO): Observation =
    Observation(
      base.id,
      base.links,
      base.note,
      None,
      observation.temperature
    )
}*/

case class EnvRequirement(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    temperature: Option[Int],
    temperatureInterval: Option[Int],
    airHumidity: Option[Int],
    airHumidityInterval: Option[Int],
    hypoxicAir: Option[Int],
    hypoxicAirInterval: Option[Int],
    cleaning: Option[String],
    light: Option[String]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object Event {
  implicit lazy val format: OFormat[Event] = flat.oformat((__ \ "type").format[String])
}
