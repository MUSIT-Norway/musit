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

import no.uio.musit.microservice.event.domain.{Dto, ObservationFromToDto}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future


object ObservationFromToDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ObservationFromToTable = TableQuery[ObservationFromToTable]

  def insertAction(event: ObservationFromToDto): DBIO[Int] =
    ObservationFromToTable += event

  def getObservationFromTo(id: Long): Future[Option[ObservationFromToDto]] =
    db.run(ObservationFromToTable.filter(event => event.id === id).result.headOption)

  private class ObservationFromToTable(tag: Tag) extends Table[ObservationFromToDto](tag, Some("MUSARK_EVENT"), "OBSERVATION_FROM_TO") {
    def * = (id, from, to) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val from = column[Option[Double]]("VALUE_FROM")
    val to = column[Option[Double]]("VALUE_TO")

    def create = (id: Option[Long], from: Option[Double], to: Option[Double]) =>
      ObservationFromToDto(id, from, to)

    def destroy(event: ObservationFromToDto) = Some(event.id, event.from, event.to)
  }

}