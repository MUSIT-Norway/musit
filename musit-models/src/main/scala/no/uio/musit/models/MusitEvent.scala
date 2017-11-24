package no.uio.musit.models

import org.joda.time.DateTime

trait MusitEvent {

  val id: Option[EventId]
  def doneBy: Option[ActorId]
  def doneDate: Option[DateTime]
  def affectedThing: Option[MusitUUID]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val updatedDate: Option[DateTime]

  def withId(id: Option[EventId]): MusitEvent

  def withAffectedThing(at: Option[MusitUUID]): MusitEvent

  def withDoneDate(dd: Option[DateTime]): MusitEvent

}

trait ModernMusitEvent extends MusitEvent {

  //These are hacks until we can remove this stuff from MusitEvent,
  // ie until we remodel storage and analysis to the same event data model as conservation
  private def fail(name: String) = {
    throw new IllegalStateException(
      s"MusitEvent.$name is not supposed to be used anymore for ConservationEvents"
    )
  }

  override def affectedThing: Option[MusitUUID] = fail("affectedThing")
  override def doneBy: Option[ActorId]          = fail("doneBy")
  override def doneDate: Option[DateTime]       = fail("doneDate")

  override def withAffectedThing(at: Option[MusitUUID]): MusitEvent =
    fail("withAffectedThing")

  override def withDoneDate(dd: Option[DateTime]): MusitEvent = fail("withDoneDate")

}
