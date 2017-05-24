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

package repositories.storage.old_dao.event

import models.storage.event.dto.EventRelation
import no.uio.musit.models.EventId

/**
 * TODO: What am I for?
 */
object EventRelationTypes {

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
  case class EventRelationDto(idFrom: EventId, relationId: Int, idTo: EventId)

}
