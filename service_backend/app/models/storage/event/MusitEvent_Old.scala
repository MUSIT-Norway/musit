package models.storage.event

import no.uio.musit.models.{ActorId, EventId, MusitId}
import org.joda.time.DateTime

/**
 * Top level representation of _all_ event types with definitions for the
 * shared attributes they all contain.
 */
trait MusitEvent_Old {
  val id: Option[EventId]
  val doneBy: Option[ActorId]
  val doneDate: DateTime
  val affectedThing: Option[MusitId]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val eventType: EventType
}

/**
 * Helps to identify events that are only valid in a "sub-event" context.
 */
trait MusitSubEvent_Old
