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

package services

import com.google.inject.Inject
import models.storage.event.EventTypeRegistry.TopLevelEvents.ObservationEventType
import models.storage.event.dto.BaseEventDto
import models.storage.event.dto.DtoConverters.ObsConverters
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults._
import no.uio.musit.models.{EventId, MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.event.EventDao

import scala.concurrent.Future

class ObservationService @Inject()(
    val eventDao: EventDao,
    val storageNodeService: StorageNodeService
) {

  val logger = Logger(classOf[ObservationService])

  /**
   * TODO: Document me!
   */
  def add(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId,
      obs: Observation
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Observation]] = {
    storageNodeService.exists(mid, nodeId).flatMap {
      case MusitSuccess(nodeExists) =>
        if (nodeExists) {
          val o = obs.copy(
            affectedThing = Some(nodeId),
            registeredBy = Some(currUsr.id),
            registeredDate = Some(dateTimeNow)
          )

          val dto = ObsConverters.observationToDto(o)
          eventDao.insertEvent(mid, dto).flatMap { eventId =>
            eventDao.getEvent(mid, eventId).map { res =>
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
        } else {
          Future.successful(MusitValidationError("Node not found."))
        }

      case err: MusitError =>
        logger.error("An error occured when trying to add an Observation")
        Future.successful(err)
    }
  }

  /**
   * TODO: Document me!
   */
  def findBy(mid: MuseumId, id: EventId): Future[MusitResult[Option[Observation]]] = {
    eventDao.getEvent(mid, id).map { result =>
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
  def listFor(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Seq[Observation]]] = {
    eventDao.getEventsForNode(mid, nodeId, ObservationEventType).map { dtos =>
      MusitSuccess(dtos.map { dto =>
        // We know we have a BaseEventDto representing an Observation.
        val base = dto.asInstanceOf[BaseEventDto]
        ObsConverters.observationFromDto(base)
      })
    }
  }

}
