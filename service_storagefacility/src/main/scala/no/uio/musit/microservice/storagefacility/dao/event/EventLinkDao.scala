/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.dao._
import no.uio.musit.microservice.storagefacility.dao.event.EventLinks._
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ EventRelation, EventRelations }
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

/**
 * Created by jstabel on 7/6/16.
 */

@Singleton
class EventLinkDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedEventTables with ColumnTypeMappers {

  private val logger = Logger(classOf[EventLinkDao])

  import driver.api._

  private val EventLinkTable = TableQuery[EventLinkTable]

  private val EventBaseTable = TableQuery[EventBaseTable]

  def insertEventLinkAction(eventLink: EventLink): DBIO[Int] = {
    insertEventLinkDtoAction(eventLink.toNormalizedEventLinkDto)
  }

  def insertEventLinkDtoAction(eventLink: EventLinkDto): DBIO[Int] = {
    logger.debug(s"inserLink: from: ${eventLink.idFrom} " +
      s"relation: ${eventLink.relationId} to: ${eventLink.idTo}")

    EventLinkTable += eventLink
  }

  def getRelatedEventDtos(parentId: Long) /*: Future[Seq[(Int, BaseEventDto)]]*/ = {
    val relevantRelations = EventLinkTable.filter(evt => evt.idFrom === parentId)

    logger.debug(s"gets relatedEventDtos for parentId: $parentId")

    val action = EventBaseTable.join(relevantRelations).on(_.id === _.idTo)

    val query = for {
      (eventBaseTable, relationTable) <- action
    } yield (relationTable.relationId, eventBaseTable)

    db.run(query.result)

  }

  implicit lazy val eventRelationMapper = MappedColumnType.base[EventRelation, Int](
    eventRelation => eventRelation.id,
    id => EventRelations.getByIdOrFail(id)
  )

  private class EventLinkTable(
      val tag: Tag
  ) extends Table[EventLinkDto](tag, SchemaName, "EVENT_RELATION_EVENT") {

    def * = (idFrom, relationId, idTo) <> (create.tupled, destroy) // scalastyle:ignore

    val idFrom = column[Long]("FROM_EVENT_ID")
    val idTo = column[Long]("TO_EVENT_ID")
    val relationId = column[Int]("RELATION_ID")

    def create = (idFrom: Long, relationId: Int, idTo: Long) =>
      EventLinkDto(
        idFrom,
        relationId,
        idTo
      )

    def destroy(link: EventLinkDto) = Some(link.idFrom, link.relationId, link.idTo)
  }

}

