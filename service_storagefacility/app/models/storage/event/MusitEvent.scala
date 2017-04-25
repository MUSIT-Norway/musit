package models.storage.event

import no.uio.musit.models.{ActorId, EventId, MusitUUID}
import org.joda.time.DateTime

trait MusitEvent { self =>

  type T

  val id: Option[EventId]
  val doneBy: Option[ActorId]
  val doneDate: DateTime
  val affectedThing: Option[MusitUUID]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val eventType: EventType

  def withId(id: Option[EventId]): T

}
