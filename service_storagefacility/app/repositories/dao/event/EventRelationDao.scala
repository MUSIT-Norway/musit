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

package repositories.dao.event

import com.google.inject.{Inject, Singleton}
import models.event.dto.BaseEventDto
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.dao.EventTables
import repositories.dao.event.EventRelationTypes.{EventRelationDto, FullEventRelation}

import scala.concurrent.Future

@Singleton
class EventRelationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  private val logger = Logger(classOf[EventRelationDao])

  import driver.api._

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

}