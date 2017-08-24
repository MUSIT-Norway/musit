package no.uio.musit.models

import org.joda.time.DateTime

trait MusitEvent {

  val id: Option[EventId]
  val doneBy: Option[ActorId]
  val doneDate: Option[DateTime]
  val affectedThing: Option[MusitUUID]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val updatedDate: Option[DateTime]

  def withId(id: Option[EventId]): MusitEvent

  def withAffectedThing(at: Option[MusitUUID]): MusitEvent

  def withDoneDate(dd: Option[DateTime]): MusitEvent

}
