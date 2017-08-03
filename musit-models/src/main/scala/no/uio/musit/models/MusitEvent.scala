package no.uio.musit.models

import org.joda.time.DateTime

trait MusitEvent {

  type T

  val id: Option[EventId]
  val doneBy: Option[ActorId]
  val doneDate: Option[DateTime]
  val affectedThing: Option[MusitUUID]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]

  def withId(id: Option[EventId]): T

}
