package models.storage.event.control

import models.storage.event.control.ControlAttributes._
import models.storage.event.{EventType, MusitEvent}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, EventId, StorageNodeId}
import org.joda.time.DateTime
import play.api.libs.json._

case class Control(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    alcohol: Option[ControlAlcohol] = None,
    cleaning: Option[ControlCleaning] = None,
    gas: Option[ControlGas] = None,
    hypoxicAir: Option[ControlHypoxicAir] = None,
    lightingCondition: Option[ControlLightingCondition] = None,
    mold: Option[ControlMold] = None,
    pest: Option[ControlPest] = None,
    relativeHumidity: Option[ControlRelativeHumidity] = None,
    temperature: Option[ControlTemperature] = None
) extends MusitEvent {

  override type T = Control

  override def withId(id: Option[EventId]) = copy(id = id)

}

object Control extends WithDateTimeFormatters {
  implicit val format: Format[Control] = Json.format[Control]
}
