package models.storage.event.old.control

import models.storage.event.{EventType, MusitEvent_Old}
import models.storage.event.old.control.ControlSubEvents._
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, EventId, StorageNodeDatabaseId}
import org.joda.time.DateTime
import play.api.libs.json._

// TODO: DELETE ME when Migration is performed in production
case class Control(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeDatabaseId],
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
) extends MusitEvent_Old

object Control extends WithDateTimeFormatters {
  implicit val format: Format[Control] = Json.format[Control]
}
