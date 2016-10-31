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

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.dao._
import no.uio.musit.microservice.storagefacility.dao.event.EventRelationTypes._
import no.uio.musit.microservice.storagefacility.domain.event.dto.BaseEventDto
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

@Singleton
class EventRelationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedEventTables with ColumnTypeMappers {

  private val logger = Logger(classOf[EventRelationDao])

  import driver.api._

  private val eventRelTable = TableQuery[EventRelationTable]
  private val eventBaseTable = TableQuery[EventBaseTable]

  def insertRelationAction(relation: FullEventRelation): DBIO[Int] = {
    insertEventRelationDtoAction(relation.toNormalizedEventLinkDto)
  }

  def insertEventRelationDtoAction(relation: EventRelationDto): DBIO[Int] = {
    logger.debug(s"inserting relation with relationId: ${relation.relationId}" +
      s" from: ${relation.idFrom} to: ${relation.idTo}")

    eventRelTable += relation
  }

  def getRelatedEvents(parentId: EventId): Future[Seq[(Int, BaseEventDto)]] = {
    val relevantRelations = eventRelTable.filter(evt => evt.fromId === parentId)

    logger.debug(s"gets relatedEventDtos for parentId: $parentId")

    val action = eventBaseTable.join(relevantRelations).on(_.id === _.toId)

    val query = for {
      (eventBaseTable, relationTable) <- action
    } yield (relationTable.relationId, eventBaseTable)

    db.run(query.result)

  }

  private class EventRelationTable(
      val tag: Tag
  ) extends Table[EventRelationDto](tag, SchemaName, "EVENT_RELATION_EVENT") {

    def * = (fromId, relationId, toId) <> (create.tupled, destroy) // scalastyle:ignore

    val fromId = column[EventId]("FROM_EVENT_ID")
    val toId = column[EventId]("TO_EVENT_ID")
    val relationId = column[Int]("RELATION_ID")

    def create = (idFrom: EventId, relationId: Int, idTo: EventId) =>
      EventRelationDto(
        idFrom,
        relationId,
        idTo
      )

    def destroy(relation: EventRelationDto) =
      Some((relation.idFrom, relation.relationId, relation.idTo))
  }

}