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

package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao
import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventService {
  def eventNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(s"Unknown event with id: $id")

  def insertAndGetNewEvent(event: Event, recursive: Boolean): MusitFuture[Event] =
    insertEvent(event).musitFutureFlatMap(newId => getEvent(newId, recursive))

  def insertEvent(event: Event): MusitFuture[Long] =
    EventDao.insertEvent(event, true).toMusitFuture

  def getEvent(id: Long, recursive: Boolean): MusitFuture[Event] =
    EventDao.getEvent(id, recursive)

  private def getEventIdsFor(eventType: EventType, relation: String, objectUri: String): MusitFuture[Seq[Long]] = {
    EventDao.getEventIds(eventType, relation, objectUri).toMusitFuture
  }

  private def getEventsFor(eventType: EventType, relation: String, objectUri: String): MusitFuture[Seq[Event]] = {
    val futEventIds = getEventIdsFor(eventType, relation, objectUri)
    MusitFuture.traverse(futEventIds)(eventId=> getEvent(eventId, true))
  }


  def getEventsFor(eventType: EventType, relation: String, id: Long): MusitFuture[Seq[Event]] = {

    val objectUri = EventRelations.getObjectUriViaRelation(id, relation).toMusitResult(MusitError(message = s"Unable to get objectUri via relation: $relation"))
    /*
    val eventTypeIdAndObjectUri: MusitResult[(Int, String)] =
      for {
        eventType <- EventType.getByNameAsMusitResult(eventType)
        objectUri <- EventRelations.getObjectUriViaRelation(id, relation).toMusitResult(MusitError(message = s"Unable to get objectUri via relation: $relation"))
      } yield (eventType.id, objectUri)
*/
    objectUri.toMusitFuture.musitFutureFlatMap {
      objectUri => getEventsFor(eventType, relation, objectUri)
    }
  }
}
