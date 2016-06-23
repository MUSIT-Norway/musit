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
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }
import play.api.libs.json.{ JsObject, Json }
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EventService {
  def eventNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(s"Unknown event with id: $id")

  def insertAndGetNewEvent(event: Event): MusitFuture[Event] = {
    insertEvent(event).musitFutureFlatMap(newId => getEvent(newId))
  }

  def insertEvent(event: Event): MusitFuture[Long] =
    EventDao.insertEvent(event).toMusitFuture //We need MusitFuture here in the future,
  //(to be able to report if user doesn't have the necessary groups etc)

  def getEvent(id: Long): MusitFuture[Event] =
    EventDao.getEvent(id).toMusitFuture(eventNotFoundError(id))

  /*

  private def getBaseEvent(id: Long): MusitFuture[Event] =
    EventDao.getBaseEvent(id).toMusitFuture(eventNotFoundError(id))

  private def getLinks(id: Long): MusitFuture[Seq[Link]] =
    LinkDao.findByLocalTableId(id).toMusitFuture

  private def getAtomLinks(id: Long): MusitFuture[Seq[Link]] =
    getLinks(id)

  def getById(id: Long): MusitFuture[Event] =
    getBaseEvent(id)
*/
}

object EventService extends EventService
