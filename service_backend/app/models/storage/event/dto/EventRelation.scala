package models.storage.event.dto

import models.storage.event._
import no.uio.musit.models._

/**
 * isNormalizedDirection is whether this direction is the same which the links
 * go in the event_relation_event table (from -> to).
 */
case class EventRelation(
    id: Int,
    name: String,
    inverseName: String,
    isNormalized: Boolean = true
) {

  def inverted = EventRelation(id, inverseName, name, !isNormalized)

  def getNormalizedDirection =
    if (isNormalized) this
    else inverted
}

/**
 * Please try to make the normalized direction reflect the "natural"
 * json/document-embedding.
 * Being consistent here may be useful for a document store (or ElasticSearch)
 * and can make it possible to optimize the generic links part during
 * json-serialization.
 */
object EventRelations {

  // The following two constants are used in the base event table to describe
  // relationship between events and sub-events.
  val PartsOfRelation   = EventRelation(1, "parts", "part_of")
  val MotivatesRelation = EventRelation(2, "motivates", "motivated_by")

  private val relations = Map(
    PartsOfRelation.id   -> PartsOfRelation,
    MotivatesRelation.id -> MotivatesRelation
  )

  /**
   * Note that this one is deliberately one-sided, we only want to find the one
   * in the "proper" direction when searching by id. Else we need separate ids
   * for the reverse relations
   */
  def unsafeGetById(id: Int): EventRelation = relations(id)

}

// =============================================================================
// Below are types describing the relations between an event and other domain
// concepts in the system.
// =============================================================================

case class EventRoleActor(
    eventId: Option[EventId],
    roleId: Int,
    actorId: ActorId
)

object EventRoleActor {

  def fromActorRole(
      actRole: ActorRole,
      eventId: Option[Long] = None
  ): EventRoleActor =
    EventRoleActor(eventId, actRole.roleId, actRole.actorId)

  def toActorRole(eventRoleActor: EventRoleActor): ActorRole =
    ActorRole(eventRoleActor.roleId, eventRoleActor.actorId)
}

case class EventRoleObject(
    eventId: Option[EventId],
    roleId: Int,
    objectId: ObjectId,
    eventTypeId: EventTypeId
)

object EventRoleObject {

  def fromObjectRole(
      objRole: ObjectRole,
      eventTypeId: EventTypeId,
      eventId: Option[Long] = None
  ): EventRoleObject =
    EventRoleObject(eventId, objRole.roleId, objRole.objectId, eventTypeId)

  def toObjectRole(eventRoleObject: EventRoleObject): ObjectRole =
    ObjectRole(eventRoleObject.roleId, eventRoleObject.objectId)
}

case class EventRolePlace(
    eventId: Option[EventId],
    roleId: Int,
    placeId: StorageNodeDatabaseId,
    eventTypeId: EventTypeId
)

object EventRolePlace {

  def fromPlaceRole(
      placeRole: PlaceRole,
      eventTypeId: EventTypeId,
      eventId: Option[Long] = None
  ): EventRolePlace =
    EventRolePlace(eventId, placeRole.roleId, placeRole.nodeId, eventTypeId)

  def toPlaceRole(eventRolePlace: EventRolePlace): PlaceRole =
    PlaceRole(eventRolePlace.roleId, eventRolePlace.placeId)
}
