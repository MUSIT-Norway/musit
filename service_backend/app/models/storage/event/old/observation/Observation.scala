package models.storage.event.old.observation

import models.storage.event.{EventType, MusitEvent_Old}
import models.storage.event.old.observation.ObservationSubEvents._
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, EventId, StorageNodeDatabaseId}
import org.joda.time.DateTime
import play.api.libs.json.{Format, _}

// TODO: DELETE ME when Migration is performed in production
case class Observation(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeDatabaseId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
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
) extends MusitEvent_Old

object Observation extends WithDateTimeFormatters {
  implicit val format: Format[Observation] = Json.format[Observation]
}
