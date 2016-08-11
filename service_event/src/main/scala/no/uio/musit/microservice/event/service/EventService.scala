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

import com.google.inject.Inject
import no.uio.musit.microservice.event.dao.EventDao
import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper

class EventService @Inject()(val eventDao: EventDao) {
  def eventNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(s"Unknown event with id: $id")

  def insertAndGetNewEvent(event: Event, recursive: Boolean): MusitFuture[Event] =
    insertEvent(event).musitFutureFlatMap(newId => getEvent(newId, recursive))

  def insertEvent(event: Event): MusitFuture[Long] =
    eventDao.insertEvent(event, true).toMusitFuture

  def getEvent(id: Long, recursive: Boolean): MusitFuture[Event] =
    eventDao.getEvent(id, recursive)
}
