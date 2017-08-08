package models.storage.event.observation

import models.storage.event.observation.ObservationAttributes._
import models.storage.event.{StorageFacilityEventType, StorageFacilityEvent}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, EventId, StorageNodeId}
import org.joda.time.DateTime
import play.api.libs.json.{Format, _}

case class Observation(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: StorageFacilityEventType,
    alcohol: Option[ObservationAlcohol] = None,
    cleaning: Option[ObservationCleaning] = None,
    gas: Option[ObservationGas] = None,
    hypoxicAir: Option[ObservationHypoxicAir] = None,
    lightingCondition: Option[ObservationLightingCondition] = None,
    mold: Option[ObservationMold] = None,
    pest: Option[ObservationPest] = None,
    relativeHumidity: Option[ObservationRelativeHumidity] = None,
    temperature: Option[ObservationTemperature] = None,
    theftProtection: Option[ObservationTheftProtection] = None,
    fireProtection: Option[ObservationFireProtection] = None,
    perimeterSecurity: Option[ObservationPerimeterSecurity] = None,
    waterDamageAssessment: Option[ObservationWaterDamageAssessment] = None
) extends StorageFacilityEvent {

  val updatedDate = None

  override type T = Observation

  override def withId(id: Option[EventId]) = copy(id = id)

}

object Observation extends WithDateTimeFormatters {
  implicit val format: Format[Observation] = Json.format[Observation]
}
