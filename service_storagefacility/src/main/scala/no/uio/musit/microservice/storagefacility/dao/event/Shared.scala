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

package no.uio.musit.microservice.storagefacility.dao.event

import no.uio.musit.microservice.storagefacility.dao.{ColumnTypeMappers, SchemaName}
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeId
import no.uio.musit.microservice.storagefacility.domain.event.dto.{BaseEventDto, EventRelation, ObservationFromToDto}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

object EventRelationTypes {

  // FIXME: Really struggling to see why the below types are necessary.

  /**
   * TODO: What am I and what is my purpose?
   */
  case class PartialEventRelation(idFrom: Long, relation: EventRelation) {
    def toFullLink(idTo: Long) = FullEventRelation(idFrom, relation, idTo)
  }

  /**
   * TODO: What am I and what is my purpose?
   */
  case class FullEventRelation(
    idFrom: Long,
    relation: EventRelation,
    idTo: Long
  ) {

    /**
     * TODO: What do I do?
     */
    def normalizedDirection = {
      if (relation.isNormalized) this
      else FullEventRelation(idTo, relation.getNormalizedDirection, idFrom)
    }

    /**
     * TODO: What do I do?
     */
    def toEventLinkDto = EventRelationDto(idFrom, relation.id, idTo)

    /**
     * TODO: What do I do?
     */
    def toNormalizedEventLinkDto = normalizedDirection.toEventLinkDto

  }

  /**
   * TODO: What am I and what is my purpose?
   */
  case class EventRelationDto(idFrom: Long, relationId: Int, idTo: Long)

}

/**
 * TODO: What am I?
 */
private[dao] trait BaseEventDao extends HasDatabaseConfigProvider[JdbcProfile]

/**
 * Tables definitions that are required across DAO implementations.
 */
private[dao] trait SharedEventTables extends BaseEventDao
  with ColumnTypeMappers {

  import driver.api._

  class EventBaseTable(
    val tag: Tag
  ) extends Table[BaseEventDto](tag, SchemaName, "EVENT") {

    // scalastyle:off method.name
    def * = (
      id.?,
      eventTypeId,
      eventNote,
      partOf,
      valueLong,
      valueString,
      valueDouble
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    val partOf = column[Option[Long]]("PART_OF")
    val valueLong = column[Option[Long]]("VALUE_LONG")
    val valueString = column[Option[String]]("VALUE_STRING")
    val valueDouble = column[Option[Double]]("VALUE_FLOAT")

    def create = (
      id: Option[Long],
      eventTypeId: EventTypeId,
      note: Option[String],
      partOf: Option[Long],
      valueLong: Option[Long],
      valueString: Option[String],
      valueDouble: Option[Double]
    ) =>
      BaseEventDto(
        id = id,
        eventTypeId = eventTypeId,
        note = note,
        relatedSubEvents = Seq.empty,
        partOf = partOf,
        valueLong = valueLong,
        valueString = valueString,
        valueDouble = valueDouble
      )

    def destroy(event: BaseEventDto) =
      Some(
        event.id,
        event.eventTypeId,
        event.note,
        event.partOf,
        event.valueLong,
        event.valueString,
        event.valueDouble
      )
  }

  class ObservationFromToTable(
    val tag: Tag
  ) extends Table[ObservationFromToDto](tag, SchemaName, "OBSERVATION_FROM_TO") {

    def * = (id, from, to) <> (create.tupled, destroy)

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val from = column[Option[Double]]("VALUE_FROM")
    val to = column[Option[Double]]("VALUE_TO")

    def create =
      (id: Option[Long], from: Option[Double], to: Option[Double]) =>
        ObservationFromToDto(id, from, to)

    def destroy(event: ObservationFromToDto) =
      Some(event.id, event.from, event.to)
  }

  val observationFromToTable = TableQuery[ObservationFromToTable]

  def insertObservationFromToAction(event: ObservationFromToDto): DBIO[Int] =
    observationFromToTable += event

  def getObservationFromTo(id: Long): Future[Option[ObservationFromToDto]] =
    db.run(
      observationFromToTable.filter(event => event.id === id).result.headOption
    )

}
