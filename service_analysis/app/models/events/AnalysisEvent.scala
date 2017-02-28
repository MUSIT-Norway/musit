package models.events

import models.events.EventResults._
import models.events.EventTypes.EventTypeId
import no.uio.musit.models.{ActorId, EventId}
import org.joda.time.DateTime
import play.api.libs.json._

/**
 * Representation of all events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventType}}}.
 */
case class AnalysisEvent(
  id: Option[EventId],
  eventTypeId: EventTypeId,
  eventDate: Option[DateTime],
  registeredBy: Option[ActorId],
  registeredDate: Option[DateTime],
  note: Option[String],
  result: Option[Result]
)

object AnalysisEvent {

  implicit val jsFormat: Format[AnalysisEvent] = Json.format[AnalysisEvent]

}

