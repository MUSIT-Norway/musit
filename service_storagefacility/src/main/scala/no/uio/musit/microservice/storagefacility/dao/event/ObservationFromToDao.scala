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

package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.event.dto.ObservationFromToDto
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
@Singleton
class ObservationFromToDao @Inject()(
  val dbConfigProvider: DatabaseConfigProvider
) extends BaseEventDao {

  import driver.api._

  private val observationFromToTable = TableQuery[ObservationFromToTable]

  /**
   * TODO: Document me!
   */
  def insertAction(event: ObservationFromToDto): DBIO[Int] =
    observationFromToTable += event

  /**
   * TODO: Document me!
   */
  def getObservationFromTo(id: Long): Future[Option[ObservationFromToDto]] =
    db.run(
      observationFromToTable.filter(event => event.id === id).result.headOption
    )

  private class ObservationFromToTable(
    val tag: Tag
  ) extends Table[ObservationFromToDto](tag, SchemaName, "OBSERVATION_FROM_TO") {
    def * = (id, from, to) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val from = column[Option[Double]]("VALUE_FROM")
    val to = column[Option[Double]]("VALUE_TO")

    def create =
      (id: Option[Long], from: Option[Double], to: Option[Double]) =>
        ObservationFromToDto(id, from, to)

    def destroy(event: ObservationFromToDto) =
      Some(event.id, event.from, event.to)
  }

}