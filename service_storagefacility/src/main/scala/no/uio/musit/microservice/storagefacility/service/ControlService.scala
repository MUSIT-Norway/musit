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

package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.event.EventDao
import no.uio.musit.microservice.storagefacility.domain.MusitResults._
import no.uio.musit.microservice.storagefacility.domain.event.EventId
import no.uio.musit.microservice.storagefacility.domain.event.control.Control
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ BaseEventDto, DtoConverters }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class ControlService @Inject() (val eventDao: EventDao) {

  /**
   *
   */
  def add(
    ctrl: Control,
    registeredBy: String
  ): Future[Long] = {
    val dto = DtoConverters.CtrlConverters.controlToDto(ctrl)
    eventDao.insertEvent(dto)
  }

  /**
   *
   * @param id
   * @return
   */
  def fetch(id: EventId): Future[MusitResult[Option[Control]]] = {
    eventDao.getEvent(id.underlying).map { result =>
      result.flatMap(_.map {
        case base: BaseEventDto =>
          MusitSuccess(
            Option(DtoConverters.CtrlConverters.controlFromDto(base))
          )

        case _ =>
          MusitInternalError(
            "Unexpected DTO type. Expected BaseEventDto with event type Control"
          )
      }.getOrElse(MusitSuccess[Option[Control]](None)))
    }
  }

}
