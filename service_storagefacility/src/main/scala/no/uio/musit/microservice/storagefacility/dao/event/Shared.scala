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

import no.uio.musit.microservice.storagefacility.dao.ColumnTypeMappers
import no.uio.musit.microservice.storagefacility.domain.event.{ EventType, EventTypeId }
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ BaseEventDto, EventRelation }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

object EventLinks {

  /**
   * TODO: What am I?
   */
  case class PartialEventLink(idFrom: Long, relation: EventRelation) {
    def toFullLink(idTo: Long) = EventLink(idFrom, relation, idTo)
  }

  /**
   * TODO: What am I?
   */
  case class EventLink(idFrom: Long, relation: EventRelation, idTo: Long) {

    /**
     * TODO: What do I do?
     */
    def normalizedDirection = {
      if (relation.isNormalized) this
      else EventLink(idTo, relation.getNormalizedDirection, idFrom)
    }

    /**
     * TODO: What do I do?
     */
    def toEventLinkDto = EventLinkDto(idFrom, relation.id, idTo)

    /**
     * TODO: What do I do?
     */
    def toNormalizedEventLinkDto = normalizedDirection.toEventLinkDto

  }

  /**
   * TODO: What am I?
   */
  case class EventLinkDto(idFrom: Long, relationId: Int, idTo: Long)

}

/**
 * Helper functions that can be shared across DAO implementations
 */
private[dao] trait EventHelpers {

  /**
   * Creates a link for a given ID to itself
   */
  def selfLink(id: Long): Link = LinkService.local(Some(id), "self", s"/v1/$id")

}

/**
 * TODO: What am I?
 */
private[dao] trait BaseEventDao extends HasDatabaseConfigProvider[JdbcProfile]

/**
 * Tables definitions that are required across DAO implementations.
 */
private[dao] trait SharedEventTables extends BaseEventDao
    with EventHelpers
    with ColumnTypeMappers {

  import driver.api._

  class EventBaseTable(tag: Tag) extends Table[BaseEventDto](tag, Some("MUSARK_STORAGE"), "EVENT") {

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
        links = Some(Seq(selfLink(id.getOrFail("EventBaseTable internal error")))),
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

}
