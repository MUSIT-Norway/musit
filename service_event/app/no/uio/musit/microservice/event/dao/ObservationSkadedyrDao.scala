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

package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.{ LivssyklusDto, ObservationSkadedyrDto }
import no.uio.musit.microservices.common.utils.DaoHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 7/8/16.
 */
object ObservationSkadedyrDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val LivssyklusTable = TableQuery[LivssyklusTable]

  // The insertAction and getObservation are somewhat more complex than necessary because I don't know how to remove the
  // EventId field from the livssyklus case class and still get it inserted using Slick. Please feel free to remove the
  // need for the EventId in the dto, that would clean this up a bit.

  def insertAction(eventId: Long, obsDto: ObservationSkadedyrDto): DBIO[Int] = {
    val livssykluserWithEventId = obsDto.livssykluser.map { livssyklus => livssyklus.copy(eventId = Some(eventId)) }
    DaoHelper.mapMultiRowInsertResultIntoOk( //See doc for mapMultiRowInsertResultIntoOk for the reason of why this is done.
      (LivssyklusTable ++= livssykluserWithEventId)
    )
  }

  def getObservation(id: Long): Future[Option[ObservationSkadedyrDto]] =
    db.run(LivssyklusTable.filter(livssyklus => livssyklus.eventId === id).result).
      map {
        seqLivssyklus =>
          if (seqLivssyklus.isEmpty) None
          else {
            val seqLivssyklusWithoutEventIds = seqLivssyklus.map(_.copy(eventId = None)) //We don't want the eventIds in json output.
            Some(ObservationSkadedyrDto(seqLivssyklusWithoutEventIds))
          }
      }

  private class LivssyklusTable(tag: Tag) extends Table[LivssyklusDto](tag, Some("MUSARK_EVENT"), "OBSERVATION_SKADEDYR_LIVSSYKLUS") {
    def * = (eventId, livssyklus, antall) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Option[Long]]("EVENT_ID")
    val livssyklus = column[Option[String]]("LIVSSYKLUS")
    val antall = column[Option[Int]]("ANTALL")

    def create = (eventId: Option[Long], livssyklus: Option[String], antall: Option[Int]) =>
      LivssyklusDto(eventId, livssyklus, antall)

    def destroy(livssyklus: LivssyklusDto) = Some(livssyklus.eventId, livssyklus.livssyklus, livssyklus.antall)
  }

}
