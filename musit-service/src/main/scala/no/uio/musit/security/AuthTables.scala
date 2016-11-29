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

package no.uio.musit.security

import java.util.UUID

import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

trait AuthTables extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  implicit lazy val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      gid => gid.asString,
      str => ActorId(UUID.fromString(str))
    )

  implicit lazy val emailMapper: BaseColumnType[Email] =
    MappedColumnType.base[Email, String](
      email => email.value,
      str => Email(str)
    )

  implicit lazy val groupIdMapper: BaseColumnType[GroupId] =
    MappedColumnType.base[GroupId, String](
      gid => gid.asString,
      str => GroupId(UUID.fromString(str))
    )

  implicit lazy val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      m => m.underlying,
      i => MuseumId.fromInt(i)
    )

  implicit lazy val permissionMapper: BaseColumnType[Permission] =
    MappedColumnType.base[Permission, Int](
      p => p.priority,
      i => Permission.fromInt(i)
    )

  //  implicit lazy val museumCollectionMapper: BaseColumnType[Seq[OldSchema]] =
  //    MappedColumnType.base[Seq[OldSchema], String](
  //      seqSchemas => seqSchemas.map(_.id).mkString("[", ",", "]"),
  //      str => OldDbSchemas.fromJsonString(str)
  //    )

  val schema = "MUSARK_AUTH"

  val grpTable = TableQuery[GroupTable]
  val usrGrpTable = TableQuery[UserGroupTable]
  val usrInfoTable = TableQuery[UserInfoTable]
  //  val musColTable = TableQuery[MuseumCollectionTable]

  type GroupTableType = ((GroupId, String, Permission, MuseumId, Option[String]))

  class GroupTable(
      val tag: Tag
  ) extends Table[GroupTableType](tag, Some(schema), "AUTH_GROUP") {

    val id = column[GroupId]("GROUP_UUID", O.PrimaryKey)
    val name = column[String]("GROUP_NAME")
    val permission = column[Permission]("GROUP_PERMISSION")
    val museumId = column[MuseumId]("GROUP_MUSEUMID")
    val description = column[Option[String]]("GROUP_DESCRIPTION")

    override def * = (id, name, permission, museumId, description) // scalastyle:ignore

  }

  //  type CollectionTableType = ((CollectionUUID, Option[String], Seq[OldSchema]))
  //
  //  class MuseumCollectionTable(
  //      val tag: Tag
  //  ) extends Table[CollectionTableType](tag, Some(schema), "MUSEUM_COLLECTION") {
  //
  //    val uuid = column[CollectionUUID]("COLLECTION_UUID", O.PrimaryKey)
  //    val name = column[Option[String]]("COLLECTION_NAME")
  //    val schemaIds = column[Seq[OldSchema]]("COLLECTION_SCHEMA_IDENTIFIERS")
  //
  //    override def * = (uuid, name, schemaIds) // scalastyle:ignore
  //
  //  }

  type UserInfoTableType = ((ActorId, Option[String], Option[String], Option[Email], Option[String])) // scalastyle:ignore

  class UserInfoTable(
      val tag: Tag
  ) extends Table[UserInfoTableType](tag, Some(schema), "USER_INFO") {

    val uuid = column[ActorId]("USER_UUID", O.PrimaryKey)
    val secId = column[Option[String]]("SECONDARY_ID")
    val name = column[Option[String]]("NAME")
    val email = column[Option[Email]]("EMAIL")
    val picture = column[Option[String]]("PICTURE")

    override def * = (uuid, secId, name, email, picture) // scalastyle:ignore

  }

  type UserGroupTableType = ((String, GroupId)) //, Option[CollectionUUID]))

  class UserGroupTable(
      val tag: Tag
  ) extends Table[UserGroupTableType](tag, Some(schema), "USER_AUTH_GROUP") {

    val feideEmail = column[String]("USER_FEIDE_EMAIL", O.PrimaryKey)
    val groupId = column[GroupId]("GROUP_UUID", O.PrimaryKey)
    //    val collectionId = column[CollectionUUID]("COLLECTION_UUID")

    def pk = primaryKey("PK_USER_GROUP", (feideEmail, groupId))

    override def * = (feideEmail, groupId) // scalastyle:ignore

  }

}
