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

import no.uio.musit.microservice.event.domain.{Dto, ObservationTemperatureDto}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future


object ObservationTemperatureDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ObservationTable = TableQuery[ObservationTable]

  def insertAction(event: ObservationTemperatureDto): DBIO[Int] =
    ObservationTable += event

  def getObservation(id: Long): Future[Option[ObservationTemperatureDto]] =
    db.run(ObservationTable.filter(event => event.id === id).result.headOption)

  private class ObservationTable(tag: Tag) extends Table[ObservationTemperatureDto](tag, Some("MUSARK_EVENT"), "OBSERVATION_TEMPERATURE") {
    def * = (id.?, temperatureFrom, temperatureTo) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey)

    val temperatureFrom = column[Option[Double]]("TEMPERATURE_FROM")
    val temperatureTo = column[Option[Double]]("TEMPERATURE_TO")

    def create = (id: Option[Long], temperatureFrom: Option[Double], temperatureTo: Option[Double]) =>
      ObservationTemperatureDto(id, temperatureFrom, temperatureTo)

    def destroy(event: ObservationTemperatureDto) = Some(event.id, event.temperatureFrom, event.temperatureTo)
  }

}