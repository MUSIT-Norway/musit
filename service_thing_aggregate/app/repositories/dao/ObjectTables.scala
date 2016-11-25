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

package repositories.dao

import models.dto.MusitObjectDto
import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

trait ObjectTables extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import driver.api._

  class ObjectTable(
      val tag: Tag
  ) extends Table[MusitObjectDto](tag, Some("MUSIT_MAPPING"), "MUSITTHING") {

    // scalastyle:off method.name
    def * = (
      museumId,
      id.?,
      museumNo,
      museumNoAsNumber,
      subNo,
      subNoAsNumber,
      term,
      mainObjectId
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[ObjectId]("OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val museumId = column[MuseumId]("MUSEUMID")
    val museumNo = column[String]("MUSEUMNO")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")
    val subNo = column[Option[String]]("SUBNO")
    val subNoAsNumber = column[Option[Long]]("SUBNOASNUMBER")
    val mainObjectId = column[Option[Long]]("MAINOBJECT_ID")
    val term = column[String]("TERM")

    def create = (
      museumId: MuseumId,
      id: Option[ObjectId],
      museumNo: String,
      museumNoAsNumber: Option[Long],
      subNo: Option[String],
      subNoAsNumber: Option[Long],
      term: String,
      mainObjectId: Option[Long]
    ) =>
      MusitObjectDto(
        museumId = museumId,
        id = id,
        museumNo = MuseumNo(museumNo),
        museumNoAsNumber = museumNoAsNumber,
        subNo = subNo.map(SubNo.apply),
        subNoAsNumber = subNoAsNumber,
        term = term,
        mainObjectId = mainObjectId
      )

    def destroy(thing: MusitObjectDto) =
      Some((
        thing.museumId,
        thing.id,
        thing.museumNo.value,
        thing.museumNoAsNumber,
        thing.subNo.map(_.value),
        thing.subNoAsNumber,
        thing.term,
        thing.mainObjectId
      ))
  }

  type LocalObject = ((ObjectId, EventId, StorageNodeId, MuseumId))

  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObject](tag, Some("MUSARK_STORAGE"), "LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * = (
      objectId,
      latestMoveId,
      currentLocationId,
      museumId
    )

    // scalastyle:on method.name

    val objectId = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeId]("CURRENT_LOCATION_ID")
    val museumId = column[MuseumId]("MUSEUM_ID")
  }
}
