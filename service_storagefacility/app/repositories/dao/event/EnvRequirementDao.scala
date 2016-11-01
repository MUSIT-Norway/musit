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

package repositories.dao.event

import com.google.inject.{Inject, Singleton}
import models.event.dto.EnvRequirementDto
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import repositories.dao.{ColumnTypeMappers, SchemaName}

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
@Singleton
class EnvRequirementDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends BaseEventDao with ColumnTypeMappers {

  import driver.api._

  private val envReqTable = TableQuery[EnvRequirementTable]

  /**
   * TODO: Document me!
   */
  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    envReqTable += event

  /**
   * TODO: Document me!
   */
  def getEnvRequirement(id: EventId): Future[Option[EnvRequirementDto]] =
    db.run(envReqTable.filter(event => event.id === id).result.headOption)

  private class EnvRequirementTable(
      tag: Tag
  ) extends Table[EnvRequirementDto](tag, SchemaName, "E_ENVIRONMENT_REQUIREMENT") {

    // scalastyle:off method.name
    def * = (
      id,
      temp,
      tempTolerance,
      relativeHumidity,
      relativeHumidityTolerance,
      hypoxicAir,
      hypoxicAirTolerance,
      cleaning,
      light
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Option[EventId]]("EVENT_ID", O.PrimaryKey)

    val temp = column[Option[Double]]("TEMPERATURE")
    val tempTolerance = column[Option[Int]]("TEMP_TOLERANCE")
    val relativeHumidity = column[Option[Double]]("REL_HUMIDITY")
    val relativeHumidityTolerance = column[Option[Int]]("REL_HUM_TOLERANCE")
    val hypoxicAir = column[Option[Double]]("HYPOXIC_AIR")
    val hypoxicAirTolerance = column[Option[Int]]("HYP_AIR_TOLERANCE")
    val cleaning = column[Option[String]]("CLEANING")
    val light = column[Option[String]]("LIGHT")

    def create = (
      id: Option[EventId],
      temp: Option[Double],
      tempTolerance: Option[Int],
      relHumidity: Option[Double],
      relHumidityTolerance: Option[Int],
      hypoxicAir: Option[Double],
      hypoxicAirTolerance: Option[Int],
      cleaning: Option[String],
      light: Option[String]
    ) =>
      EnvRequirementDto(
        id = id,
        temperature = temp,
        tempTolerance = tempTolerance,
        airHumidity = relHumidity,
        airHumTolerance = relHumidityTolerance,
        hypoxicAir = hypoxicAir,
        hypoxicTolerance = hypoxicAirTolerance,
        cleaning = cleaning,
        light = light
      )

    def destroy(envReq: EnvRequirementDto) =
      Some((
        envReq.id,
        envReq.temperature,
        envReq.tempTolerance,
        envReq.airHumidity,
        envReq.airHumTolerance,
        envReq.hypoxicAir,
        envReq.hypoxicTolerance,
        envReq.cleaning,
        envReq.light
      ))
  }

}

