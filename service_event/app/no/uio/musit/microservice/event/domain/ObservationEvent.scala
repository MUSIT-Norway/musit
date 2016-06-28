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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dao.ObservationDAO
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.json.{ JsObject, JsResult, JsValue, Json }
import slick.dbio._
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ObservationDTO(id: Option[Long], temperature: Option[Double])

object ObservationDTO {
  implicit val format = Json.format[ObservationDTO]
}

class Observation(eventType: EventType, baseDTO: BaseEventDTO, val observationDTO: ObservationDTO) extends Event(eventType, baseDTO) {
  val temperature = observationDTO.temperature
}

object Observation extends EventFactory {

  override def fromJson(eventType: EventType, baseResult: JsResult[BaseEventDTO], jsObject: JsObject): JsResult[Observation] = {
    for {
      baseDto <- baseResult
      observationEventDto <- jsObject.validate[ObservationDTO]
    } yield new Observation(eventType, baseDto, observationEventDto)
  }

  def toJson(event: Event): JsValue = Json.toJson(event.asInstanceOf[Observation].observationDTO)

  def fromDatabase(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Observation] = {
    val maybeObservation = ObservationDAO.getObservation(id).toMusitFuture(ErrorHelper.badRequest(s"Unable to find observation with id: $id"))
    maybeObservation.musitFutureMap(observationDTO => new Observation(eventType, baseEventDto, observationDTO))
  }

  def createDatabaseInsertAction(id: Long, event: Event): DBIO[Int] = ObservationDAO.insertAction(id, event.asInstanceOf[Observation])
}
