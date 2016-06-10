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

import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }
import no.uio.musit.microservice.event.domain.EventInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 6/10/16.
 */
trait EventService {

  //A separate function for this message because we want to verify we get this error message in some of the integration tests
  def unknownEventMsg(id: Long) = s"Unknown event with id: $id"

  private def eventNotFoundError(id: Long): MusitError = {
    ErrorHelper.notFound(unknownEventMsg(id))
  }

  def createEvent(eventInfo: EventInfo): Future[Either[MusitError, EventInfo]] = {
    ErrorHelper.futureNotImplemented("EventService.create not implemented yet")
  }
}

object EventService extends EventService
