/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.microservice.storagefacility.domain.event.dto

import no.uio.musit.microservice.storagefacility.domain.event.{ ActorRole, ObjectRole, PlaceRole }

/**
 * Created by jstabel on 7/6/16.
 */

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

  def getNormalizedDirection =
    if (isNormalized) this
    else EventRelations.unsafeGetByName(inverseName)
}

/**
 * Please try to make the normalized direction reflect the "natural"
 * json/document-embedding.
 * Being consistent here may be useful for a document store (or ElasticSearch)
 * and can make it possible to optimize the generic links part during
 * json-serialization.
 */
object EventRelations {

  val PartsOfRelation = EventRelation(1, "parts", "part_of")
  val MotivatesRelation = EventRelation(2, "motivates", "motivated_by")

  private val relations = Seq(PartsOfRelation, MotivatesRelation)

  private val bothSidesRelations =
    relations ++ relations.map { rel =>
      EventRelation(rel.id, rel.inverseName, rel.name, !rel.isNormalized)
    }

  private val relationByName: Map[String, EventRelation] =
    bothSidesRelations.map(rel => rel.name.toLowerCase -> rel).toMap

  // Note that this one is deliberately one-sided, we only want to find the one
  // in the "proper" direction when searching by id. Else we need separate ids
  // for the reverse relations
  private val relationById: Map[Int, EventRelation] =
    relations.map(rel => rel.id -> rel).toMap

  // This one is hardcoded some places in the system, because it is treated in a
  // special way (not stored in the event-relation-table, but in the base
  // event-table directly
  val relation_parts = unsafeGetByName("parts")

  // Shouldn't be used in the main framework, but perhaps used by tests and
  // some other logic
  val relation_motivates = unsafeGetByName("motivates")

  def getByName(name: String) = relationByName.get(name.toLowerCase)

  def getById(id: Int): Option[EventRelation] = relationById.get(id)

  def unsafeGetById(id: Int): EventRelation = getById(id).get

  def unsafeGetByName(name: String): EventRelation = getByName(name).get
}

/*
    Below are types describing the relations between an event and other
    domain concepts in the system.
 */

case class EventRoleActor(eventId: Option[Long], roleId: Int, actorId: Int)

object EventRoleActor {
  def toEventRoleActor(actRole: ActorRole, eventId: Option[Long] = None) =
    EventRoleActor(eventId, actRole.roleId, actRole.actorId)

  def toActorRole(eventRoleActor: EventRoleActor) =
    ActorRole(eventRoleActor.roleId, eventRoleActor.actorId)
}

case class EventRoleObject(eventId: Option[Long], roleId: Int, objectId: Long)

object EventRoleObject {

  def toEventRoleObject(objRole: ObjectRole, eventId: Option[Long] = None) =
    EventRoleObject(eventId, objRole.roleId, objRole.objectId)

  def toObjectRole(eventRoleObject: EventRoleObject) =
    ObjectRole(eventRoleObject.roleId, eventRoleObject.objectId)
}

case class EventRolePlace(eventId: Option[Long], roleId: Int, placeId: Int)

object EventRolePlace {

  def toEventRolePlace(placeRole: PlaceRole, eventId: Option[Long] = None) =
    EventRolePlace(eventId, placeRole.roleId, placeRole.placeId)

  def toPlaceRole(eventRolePlace: EventRolePlace) =
    PlaceRole(eventRolePlace.roleId, eventRolePlace.placeId)
}