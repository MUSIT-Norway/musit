package models.storage.event.envreq

import models.storage.Interval
import models.storage.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import models.storage.event.{EventType, MusitEvent}
import models.storage.nodes.EnvironmentRequirement
import no.uio.musit.models.{ActorId, EventId, StorageNodeId}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

case class EnvRequirement(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    note: Option[String],
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    temperature: Option[Interval[Double]],
    airHumidity: Option[Interval[Double]],
    hypoxicAir: Option[Interval[Double]],
    cleaning: Option[String],
    light: Option[String]
) extends MusitEvent {

  override type T = EnvRequirement

  override def withId(id: Option[EventId]) = copy(id = id)

  def similar(er: EnvRequirement): Boolean = {
    // Compare the basic similarities of the environment requirements
    affectedThing == er.affectedThing &&
    temperature == er.temperature &&
    airHumidity == er.airHumidity &&
    hypoxicAir == er.hypoxicAir &&
    cleaning == er.cleaning &&
    light == er.light &&
    note == er.note
  }

}

object EnvRequirement {

  implicit val format: Format[EnvRequirement] = Json.format[EnvRequirement]

  /**
   * Convert an EnvironmentRequirement type into an EnvRequirement event.
   *
   * @param doneBy         The ActorId of the currently logged in user.
   * @param affectedNodeId The StorageNodeId the event applies to
   * @param now            The current timestamp.
   * @param er             EnvironmentRequirement to convert
   * @return an EnvRequirement instance
   */
  def toEnvRequirementEvent(
      doneBy: ActorId,
      affectedNodeId: StorageNodeId,
      now: DateTime,
      er: EnvironmentRequirement
  ): EnvRequirement = {
    EnvRequirement(
      id = None,
      doneBy = Some(doneBy),
      doneDate = now,
      note = er.comment,
      affectedThing = Some(affectedNodeId),
      registeredBy = Some(doneBy),
      registeredDate = Some(now),
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = er.temperature,
      airHumidity = er.relativeHumidity,
      hypoxicAir = er.hypoxicAir,
      cleaning = er.cleaning,
      light = er.lightingCondition
    )
  }

  def fromEnvRequirementEvent(er: EnvRequirement): EnvironmentRequirement = {
    EnvironmentRequirement(
      temperature = er.temperature,
      relativeHumidity = er.airHumidity,
      hypoxicAir = er.hypoxicAir,
      cleaning = er.cleaning,
      lightingCondition = er.light,
      comment = er.note
    )
  }

}
