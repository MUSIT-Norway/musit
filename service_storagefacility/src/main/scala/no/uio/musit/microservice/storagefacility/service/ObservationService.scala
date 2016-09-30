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
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.{ EventId, ObjectRole }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.ObservationEventType
import no.uio.musit.microservice.storagefacility.domain.event.dto.BaseEventDto
import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters.ObsConverters
import no.uio.musit.microservice.storagefacility.domain.event.observation.Observation
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class ObservationService @Inject() (val eventDao: EventDao) {

  val logger = Logger(classOf[ObservationService])

  /**
   * TODO: Document me!
   */
  def add(
    nodeId: Long,
    obs: Observation
  )(implicit currUsr: String): Future[MusitResult[Observation]] = {
    val o = obs.copy(
      baseEvent = obs.baseEvent.copy(
        affectedThing = Some(ObjectRole(
          roleId = 1,
          objectId = nodeId
        )),
        registeredBy = Some(currUsr),
        registeredDate = Some(dateTimeNow)
      )
    )
    val dto = ObsConverters.observationToDto(o)
    eventDao.insertEvent(dto).flatMap { eventId =>
      eventDao.getEvent(eventId).map { res =>
        res.flatMap(_.map { dto =>
          // We know we have a BaseEventDto representing an Observation.
          val bdto = dto.asInstanceOf[BaseEventDto]
          MusitSuccess(ObsConverters.observationFromDto(bdto))
        }.getOrElse {
          logger.error(
            s"An unexpected error occured when trying to fetch an " +
              s"observation event that was added with eventId $eventId"
          )
          MusitInternalError("Could not locate the observation that was added")
        })
      }
    }
  }

  /**
   * TODO: Document me!
   */
  def findBy(id: EventId): Future[MusitResult[Option[Observation]]] = {
    eventDao.getEvent(id.underlying).map { result =>
      result.flatMap(_.map {
        case base: BaseEventDto =>
          MusitSuccess(
            Option(ObsConverters.observationFromDto(base))
          )

        case _ =>
          MusitInternalError(
            "Unexpected DTO type. Expected BaseEventDto with event type Observation"
          )
      }.getOrElse(MusitSuccess(None)))
    }
  }

  /**
   * TODO: Document me!
   */
  def listFor(nodeId: StorageNodeId): Future[MusitResult[Seq[Observation]]] = {
    eventDao.getEventsForNode(nodeId, ObservationEventType).map { dtos =>
      MusitSuccess(dtos.map { dto =>
        // We know we have a BaseEventDto representing an Observation.
        val base = dto.asInstanceOf[BaseEventDto]
        ObsConverters.observationFromDto(base)
      })
    }
  }

}
