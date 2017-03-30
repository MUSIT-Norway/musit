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

package repositories.storage.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * This Dao should _NOT_ be used by any other class than the bootstrapping
 * {{{migration.UUIDVerifier}}} class.
 */
@Singleton
class MigrationDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends StorageTables {

  val logger = Logger(classOf[MigrationDao])

  import profile.api._

  // scalastyle:off
  type ObjectRow = (
      (
          Option[ObjectId],
          Option[ObjectUUID],
          MuseumId,
          String,
          Option[Long],
          Option[String],
          Option[Long],
          Option[Long],
          Boolean,
          String,
          Option[String],
          Option[Long],
          Option[Int]
      )
  )
  // scalastyle:on

  val objTable = TableQuery[ObjectTable]

  def getObjectUUIDsForObjectIds(
      ids: Seq[ObjectId]
  ): Future[Map[ObjectId, (ObjectUUID, MuseumId)]] = {
    val q = objTable.filter(_.id inSet ids).map(n => (n.id, n.uuid, n.museumId))
    db.run(q.result).map(tuples => tuples.map(t => (t._1, (t._2.get, t._3))).toMap)
  }

  // We can do this, since there are not that many nodes.
  def getAllNodeIds: Future[Map[StorageNodeDatabaseId, (StorageNodeId, MuseumId)]] = {
    val q = storageNodeTable.map(n => (n.id, n.uuid, n.museumId))
    db.run(q.result).map(tuples => tuples.map(t => (t._1, (t._2.get, t._3))).toMap)
  }

  /**
   * Definition for the MUSIT_MAPPING.MUSITTHING table
   */
  class ObjectTable(
      val tag: Tag
  ) extends Table[ObjectRow](tag, Some("MUSIT_MAPPING"), "MUSITTHING") {

    // scalastyle:off method.name
    def * = (
      id.?,
      uuid,
      museumId,
      museumNo,
      museumNoAsNumber,
      subNo,
      subNoAsNumber,
      mainObjectId,
      isDeleted,
      term,
      oldSchema,
      oldObjId,
      newCollectionId
    )

    // scalastyle:on method.name

    val id               = column[ObjectId]("OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val uuid             = column[Option[ObjectUUID]]("MUSITTHING_UUID")
    val museumId         = column[MuseumId]("MUSEUMID")
    val museumNo         = column[String]("MUSEUMNO")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")
    val subNo            = column[Option[String]]("SUBNO")
    val subNoAsNumber    = column[Option[Long]]("SUBNOASNUMBER")
    val mainObjectId     = column[Option[Long]]("MAINOBJECT_ID")
    val isDeleted        = column[Boolean]("IS_DELETED")
    val term             = column[String]("TERM")
    val oldSchema        = column[Option[String]]("OLD_SCHEMANAME")
    val oldObjId         = column[Option[Long]]("LOKAL_PK")
    val oldBarcode       = column[Option[Long]]("OLD_BARCODE")
    val newCollectionId  = column[Option[Int]]("NEW_COLLECTION_ID")

  }

}
