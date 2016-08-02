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

import no.uio.musit.microservice.event.domain.{ ObservationSpritDto, TilstandDto }
import no.uio.musit.microservices.common.utils.DaoHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by sveigl on 8/1/16.
 */
object ObservationSpritDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val TilstandTable = TableQuery[TilstandTable]

  // The insertAction and getObservation are somewhat more complex than necessary because I don't know how to remove the
  // EventId field from the tilstand case class and still get it inserted using Slick. Please feel free to remove the
  // need for the EventId in the dto, that would clean this up a bit.

  def insertAction(eventId: Long, obsDto: ObservationSpritDto): DBIO[Int] = {
    val tilstanderWithEventId = obsDto.tilstander.map { tilstand => tilstand.copy(eventId = Some(eventId)) }
    DaoHelper.mapMultiRowInsertResultIntoOk(
      TilstandTable ++= tilstanderWithEventId
    )
  }

  def getObservation(id: Long): Future[Option[ObservationSpritDto]] =
    db.run(TilstandTable.filter(tilstand => tilstand.eventId === id).result).
      map {
        seqTilstand =>
          if (seqTilstand.isEmpty) {
            None
          } else {
            val seqTilstandWithoutEventIds = seqTilstand.map(_.copy(eventId = None)) //We don't want the eventIds in json output.
            Some(ObservationSpritDto(seqTilstandWithoutEventIds))
          }
      }

  private class TilstandTable(tag: Tag) extends Table[TilstandDto](tag, Some("MUSARK_EVENT"), "OBSERVATION_SPRIT_TILSTAND") {
    def * = (eventId, tilstand, volum) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Option[Long]]("EVENT_ID")
    val tilstand = column[Option[String]]("TILSTAND")
    val volum = column[Option[Double]]("VOLUM")

    def create = (eventId: Option[Long], tilstand: Option[String], volum: Option[Double]) =>
      TilstandDto(eventId, tilstand, volum)

    def destroy(tilstand: TilstandDto) = Some(tilstand.eventId, tilstand.tilstand, tilstand.volum)
  }

}
