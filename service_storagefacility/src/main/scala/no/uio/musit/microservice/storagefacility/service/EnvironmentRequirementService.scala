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
import no.uio.musit.microservice.storagefacility.dao.event.{ EnvRequirementDao, EventDao }
import no.uio.musit.microservice.storagefacility.domain.MusitResults.{ MusitInternalError, MusitResult, MusitSuccess }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import no.uio.musit.microservice.storagefacility.domain.event.{ EventId, EventType }
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ EventDto, ExtendedDto }
import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters.EnvReqConverters
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.storage.{ EnvironmentRequirement, StorageNodeId }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class EnvironmentRequirementService @Inject() (
    val eventDao: EventDao,
    val envRequirementDao: EnvRequirementDao
) {

  val logger = Logger(classOf[EnvironmentRequirementService])

  private val unexpectedType = MusitInternalError(
    "Unexpected DTO type. Expected ExtendedDto with event type EnvRequirement"
  )

  def add(envReq: EnvRequirement)(implicit currUsr: String): Future[MusitResult[EnvRequirement]] = {
    // TODO: Need to check if the previous envreq is the same as this one.
    val dto = EnvReqConverters.envReqToDto(envReq)
    eventDao.insertEvent(dto).flatMap { eventId =>
      eventDao.getEvent(eventId).map { res =>
        res.flatMap(_.map { dto =>
          // We know we have an ExtendedDto representing an EnvRequirement
          val extDto = dto.asInstanceOf[ExtendedDto]
          MusitSuccess(EnvReqConverters.envReqFromDto(extDto))
        }.getOrElse {
          logger.error(
            s"Unexpected error when trying to fetch an environment" +
              s" requirement event that was added with eventId $eventId"
          )
          MusitInternalError("Could not locate the EnvRequirement that was added")
        })
      }
    }
  }

  def findBy(id: EventId): Future[MusitResult[Option[EnvRequirement]]] = {
    eventDao.getEvent(id.underlying).map { result =>
      convertResult(result)
    }
  }

  def findLatestForNodeId(
    nodeId: StorageNodeId
  ): Future[MusitResult[Option[EnvironmentRequirement]]] = {
    eventDao.latestByNodeId(nodeId, EnvRequirementEventType.id).map { result =>
      convertResult(result).map { maybeEvt =>
        maybeEvt.map(EnvRequirement.fromEnvRequirementEvent)
      }
    }
  }

  private def convertResult(result: MusitResult[Option[EventDto]]) = {
    result.flatMap(_.map {
      case ext: ExtendedDto =>
        MusitSuccess(
          Option(EnvReqConverters.envReqFromDto(ext))
        )

      case _ =>
        unexpectedType
    }.getOrElse(MusitSuccess(None)))
  }

}
