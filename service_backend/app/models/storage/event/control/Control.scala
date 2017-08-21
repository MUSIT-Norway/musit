package models.storage.event.control

import models.storage.event.control.ControlAttributes._
import models.storage.event.{StorageFacilityEvent, StorageFacilityEventType}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

case class Control(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: StorageFacilityEventType,
    alcohol: Option[ControlAlcohol] = None,
    cleaning: Option[ControlCleaning] = None,
    gas: Option[ControlGas] = None,
    hypoxicAir: Option[ControlHypoxicAir] = None,
    lightingCondition: Option[ControlLightingCondition] = None,
    mold: Option[ControlMold] = None,
    pest: Option[ControlPest] = None,
    relativeHumidity: Option[ControlRelativeHumidity] = None,
    temperature: Option[ControlTemperature] = None
) extends StorageFacilityEvent {

  override val updatedDate = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case nid: StorageNodeId => copy(affectedThing = Some(nid))
    case _                  => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object Control extends WithDateTimeFormatters {
  implicit val format: Format[Control] = Json.format[Control]
}
