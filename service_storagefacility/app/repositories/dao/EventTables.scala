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

package repositories.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.event.EventTypeId
import models.event.dto._
import no.uio.musit.models.{ActorId, EventId, ObjectId, StorageNodeDatabaseId}
import repositories.dao.event.EventRelationTypes.EventRelationDto

/**
 * Tables definitions that are required across DAO implementations.
 */
private[dao] trait EventTables extends BaseDao with ColumnTypeMappers {

  import driver.api._

  val obsFromToTable       = TableQuery[ObservationFromToTable]
  val eventBaseTable       = TableQuery[EventBaseTable]
  val eventRelTable        = TableQuery[EventRelationTable]
  val lifeCycleTable       = TableQuery[LifeCycleTable]
  val envReqTable          = TableQuery[EnvRequirementTable]
  val eventObjectsTable    = TableQuery[EventObjectsTable]
  val placesAsObjectsTable = TableQuery[EventPlacesAsObjectsTable]
  val eventPlacesTable     = TableQuery[EventPlacesTable]
  val eventActorsTable     = TableQuery[EventActorsTable]

  class EventBaseTable(
      val tag: Tag
  ) extends Table[BaseEventDto](tag, SchemaName, "EVENT") {

    // scalastyle:off method.name
    def * =
      (
        id.?,
        eventTypeId,
        eventDate,
        eventNote,
        partOf,
        valueLong,
        valueString,
        valueDouble,
        registeredBy,
        registeredDate
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id             = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId    = column[EventTypeId]("EVENT_TYPE_ID")
    val eventDate      = column[JSqlTimestamp]("EVENT_DATE")
    val eventNote      = column[Option[String]]("NOTE")
    val partOf         = column[Option[EventId]]("PART_OF")
    val valueLong      = column[Option[Long]]("VALUE_LONG")
    val valueString    = column[Option[String]]("VALUE_STRING")
    val valueDouble    = column[Option[Double]]("VALUE_FLOAT")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")

    def create =
      (
          id: Option[EventId],
          eventTypeId: EventTypeId,
          eventDate: JSqlTimestamp,
          note: Option[String],
          partOf: Option[EventId],
          valueLong: Option[Long],
          valueString: Option[String],
          valueDouble: Option[Double],
          registeredBy: Option[ActorId],
          registeredDate: Option[JSqlTimestamp]
      ) =>
        BaseEventDto(
          id = id,
          eventTypeId = eventTypeId,
          eventDate = eventDate,
          relatedActors = Seq.empty,
          relatedObjects = Seq.empty,
          relatedPlaces = Seq.empty,
          note = note,
          relatedSubEvents = Seq.empty,
          partOf = partOf,
          valueLong = valueLong,
          valueString = valueString,
          valueDouble = valueDouble,
          registeredBy = registeredBy,
          registeredDate = registeredDate
      )

    def destroy(event: BaseEventDto) =
      Some(
        (
          event.id,
          event.eventTypeId,
          event.eventDate,
          event.note,
          event.partOf,
          event.valueLong,
          event.valueString,
          event.valueDouble,
          event.registeredBy,
          event.registeredDate
        )
      )
  }

  class EventRelationTable(
      val tag: Tag
  ) extends Table[EventRelationDto](tag, SchemaName, "EVENT_RELATION_EVENT") {

    def * = (fromId, relationId, toId) <> (create.tupled, destroy) // scalastyle:ignore

    val fromId     = column[EventId]("FROM_EVENT_ID")
    val toId       = column[EventId]("TO_EVENT_ID")
    val relationId = column[Int]("RELATION_ID")

    def create =
      (idFrom: EventId, relationId: Int, idTo: EventId) =>
        EventRelationDto(
          idFrom,
          relationId,
          idTo
      )

    def destroy(relation: EventRelationDto) =
      Some((relation.idFrom, relation.relationId, relation.idTo))
  }

  class ObservationFromToTable(
      val tag: Tag
  ) extends Table[ObservationFromToDto](tag, SchemaName, "OBSERVATION_FROM_TO") {

    def * = (id, from, to) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[EventId]]("EVENT_ID", O.PrimaryKey)

    val from = column[Option[Double]]("VALUE_FROM")
    val to   = column[Option[Double]]("VALUE_TO")

    def create =
      (id: Option[EventId], from: Option[Double], to: Option[Double]) =>
        ObservationFromToDto(id, from, to)

    def destroy(event: ObservationFromToDto) =
      Some((event.id, event.from, event.to))
  }

  class LifeCycleTable(
      val tag: Tag
  ) extends Table[LifecycleDto](tag, SchemaName, "OBSERVATION_PEST_LIFECYCLE") {
    def * = (eventId, stage, quantity) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId  = column[Option[EventId]]("EVENT_ID")
    val stage    = column[Option[String]]("STAGE")
    val quantity = column[Option[Int]]("QUANTITY")

    def create =
      (eventId: Option[EventId], stage: Option[String], quantity: Option[Int]) =>
        LifecycleDto(eventId, stage, quantity)

    def destroy(lifeCycle: LifecycleDto) =
      Some((lifeCycle.eventId, lifeCycle.stage, lifeCycle.quantity))
  }

  class EnvRequirementTable(
      tag: Tag
  ) extends Table[EnvRequirementDto](tag, SchemaName, "E_ENVIRONMENT_REQUIREMENT") {

    // scalastyle:off method.name
    def * =
      (
        id,
        temp,
        tempTolerance,
        relativeHumidity,
        relativeHumidityTolerance,
        hypoxicAir,
        hypoxicAirTolerance,
        cleaning,
        light
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Option[EventId]]("EVENT_ID", O.PrimaryKey)

    val temp                      = column[Option[Double]]("TEMPERATURE")
    val tempTolerance             = column[Option[Int]]("TEMP_TOLERANCE")
    val relativeHumidity          = column[Option[Double]]("REL_HUMIDITY")
    val relativeHumidityTolerance = column[Option[Int]]("REL_HUM_TOLERANCE")
    val hypoxicAir                = column[Option[Double]]("HYPOXIC_AIR")
    val hypoxicAirTolerance       = column[Option[Int]]("HYP_AIR_TOLERANCE")
    val cleaning                  = column[Option[String]]("CLEANING")
    val light                     = column[Option[String]]("LIGHT")

    def create =
      (
          id: Option[EventId],
          temp: Option[Double],
          tempTolerance: Option[Int],
          relHumidity: Option[Double],
          relHumidityTolerance: Option[Int],
          hypoxicAir: Option[Double],
          hypoxicAirTolerance: Option[Int],
          cleaning: Option[String],
          light: Option[String]
      ) =>
        EnvRequirementDto(
          id = id,
          temperature = temp,
          tempTolerance = tempTolerance,
          airHumidity = relHumidity,
          airHumTolerance = relHumidityTolerance,
          hypoxicAir = hypoxicAir,
          hypoxicTolerance = hypoxicAirTolerance,
          cleaning = cleaning,
          light = light
      )

    def destroy(envReq: EnvRequirementDto) =
      Some(
        (
          envReq.id,
          envReq.temperature,
          envReq.tempTolerance,
          envReq.airHumidity,
          envReq.airHumTolerance,
          envReq.hypoxicAir,
          envReq.hypoxicTolerance,
          envReq.cleaning,
          envReq.light
        )
      )
  }

  class EventObjectsTable(
      tag: Tag
  ) extends Table[EventRoleObject](tag, SchemaName, "EVENT_ROLE_OBJECT") {

    def * =
      (eventId.?, roleId, objectId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId     = column[EventId]("EVENT_ID")
    val roleId      = column[Int]("ROLE_ID")
    val objectId    = column[ObjectId]("OBJECT_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create =
      (
          eventId: Option[EventId],
          roleId: Int,
          objectId: ObjectId,
          eventTypeId: EventTypeId
      ) =>
        EventRoleObject(
          eventId = eventId,
          roleId = roleId,
          objectId = objectId,
          eventTypeId = eventTypeId
      )

    def destroy(eventRoleObject: EventRoleObject) =
      Some(
        (
          eventRoleObject.eventId,
          eventRoleObject.roleId,
          eventRoleObject.objectId,
          eventRoleObject.eventTypeId
        )
      )
  }

  class EventPlacesAsObjectsTable(
      val tag: Tag
  ) extends Table[EventRolePlace](tag, SchemaName, "EVENT_ROLE_PLACE_AS_OBJECT") {
    def * =
      (eventId.?, roleId, placeId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId     = column[EventId]("EVENT_ID")
    val roleId      = column[Int]("ROLE_ID")
    val placeId     = column[StorageNodeDatabaseId]("PLACE_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create =
      (
          eventId: Option[EventId],
          roleId: Int,
          placeId: StorageNodeDatabaseId,
          eventTypeId: EventTypeId
      ) =>
        EventRolePlace(
          eventId = eventId,
          roleId = roleId,
          placeId = placeId,
          eventTypeId = eventTypeId
      )

    def destroy(eventRolePlace: EventRolePlace) =
      Some(
        (
          eventRolePlace.eventId,
          eventRolePlace.roleId,
          eventRolePlace.placeId,
          eventRolePlace.eventTypeId
        )
      )
  }

  class EventPlacesTable(
      tag: Tag
  ) extends Table[EventRolePlace](tag, SchemaName, "EVENT_ROLE_PLACE") {

    def * =
      (eventId.?, roleId, placeId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId     = column[EventId]("EVENT_ID")
    val roleId      = column[Int]("ROLE_ID")
    val placeId     = column[StorageNodeDatabaseId]("PLACE_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create =
      (
          eventId: Option[EventId],
          roleId: Int,
          placeId: StorageNodeDatabaseId,
          eventTypeId: EventTypeId
      ) =>
        EventRolePlace(
          eventId = eventId,
          roleId = roleId,
          placeId = placeId,
          eventTypeId = eventTypeId
      )

    def destroy(eventRolePlace: EventRolePlace) =
      Some(
        (
          eventRolePlace.eventId,
          eventRolePlace.roleId,
          eventRolePlace.placeId,
          eventRolePlace.eventTypeId
        )
      )
  }

  class EventActorsTable(
      tag: Tag
  ) extends Table[EventRoleActor](tag, SchemaName, "EVENT_ROLE_ACTOR") {

    def * = (eventId.?, roleId, actorId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[EventId]("EVENT_ID")
    val roleId  = column[Int]("ROLE_ID")
    val actorId = column[ActorId]("ACTOR_UUID")

    def create =
      (eventId: Option[EventId], roleId: Int, actorId: ActorId) =>
        EventRoleActor(
          eventId = eventId,
          roleId = roleId,
          actorId = actorId
      )

    def destroy(eventRoleActor: EventRoleActor) =
      Some(
        (
          eventRoleActor.eventId,
          eventRoleActor.roleId,
          eventRoleActor.actorId
        )
      )
  }

}
