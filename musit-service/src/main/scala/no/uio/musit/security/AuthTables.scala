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

import java.sql.{Timestamp => JSqlTimestamp}

import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.OldDbSchemas.OldSchema
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.time.DateTimeImplicits
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait AuthTables extends HasDatabaseConfigProvider[JdbcProfile]
  with DateTimeImplicits {

  private val logger = Logger(classOf[AuthTables])

  import driver.api._

  implicit lazy val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      gid => gid.asString,
      str => ActorId.unsafeFromString(str)
    )

  implicit lazy val emailMapper: BaseColumnType[Email] =
    MappedColumnType.base[Email, String](
      email => email.value,
      str => Email(str)
    )

  implicit lazy val collectionIdMapper: BaseColumnType[CollectionUUID] =
    MappedColumnType.base[CollectionUUID, String](
      cid => cid.asString,
      str => CollectionUUID.unsafeFromString(str)
    )

  implicit lazy val groupIdMapper: BaseColumnType[GroupId] =
    MappedColumnType.base[GroupId, String](
      gid => gid.asString,
      str => GroupId.unsafeFromString(str)
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

  implicit lazy val oldSchemaMapper: BaseColumnType[Seq[OldSchema]] =
    MappedColumnType.base[Seq[OldSchema], String](
      seqSchemas => seqSchemas.map(_.id).mkString("[", ",", "]"),
      str => OldDbSchemas.fromJsonString(str)
    )

  implicit lazy val sessionIdMapper: BaseColumnType[SessionUUID] =
    MappedColumnType.base[SessionUUID, String](
      sid => sid.asString,
      str => SessionUUID.unsafeFromString(str)
    )

  implicit lazy val bearerTokenMapper: BaseColumnType[BearerToken] =
    MappedColumnType.base[BearerToken, String](
      bt => bt.underlying,
      str => BearerToken(str)
    )

  val schema = "MUSARK_AUTH"

  val grpTable = TableQuery[GroupTable]
  val usrGrpTable = TableQuery[UserGroupTable]
  val usrInfoTable = TableQuery[UserInfoTable]
  val musColTable = TableQuery[MuseumCollectionTable]
  val usrSessionTable = TableQuery[UserSessionTable]

  type GroupDBTuple = ((GroupId, String, Permission, MuseumId, Option[String]))

  class GroupTable(
      val tag: Tag
  ) extends Table[GroupDBTuple](tag, Some(schema), "AUTH_GROUP") {

    val id = column[GroupId]("GROUP_UUID", O.PrimaryKey)
    val name = column[String]("GROUP_NAME")
    val permission = column[Permission]("GROUP_PERMISSION")
    val museumId = column[MuseumId]("GROUP_MUSEUMID")
    val description = column[Option[String]]("GROUP_DESCRIPTION")

    override def * = (id, name, permission, museumId, description) // scalastyle:ignore

  }

  type CollectionDBTuple = ((CollectionUUID, Option[String], Seq[OldSchema]))

  class MuseumCollectionTable(
      val tag: Tag
  ) extends Table[CollectionDBTuple](tag, Some(schema), "MUSEUM_COLLECTION") {

    val uuid = column[CollectionUUID]("COLLECTION_UUID", O.PrimaryKey)
    val name = column[Option[String]]("COLLECTION_NAME")
    val schemaIds = column[Seq[OldSchema]]("COLLECTION_SCHEMA_IDENTIFIERS")

    override def * = (uuid, name, schemaIds) // scalastyle:ignore

  }

  type UserInfoDBTuple = ((ActorId, Option[Email], Option[String], Option[Email], Option[String])) // scalastyle:ignore

  class UserInfoTable(
      val tag: Tag
  ) extends Table[UserInfoDBTuple](tag, Some(schema), "USER_INFO") {

    val uuid = column[ActorId]("USER_UUID", O.PrimaryKey)
    val secId = column[Option[Email]]("SECONDARY_ID")
    val name = column[Option[String]]("NAME")
    val email = column[Option[Email]]("EMAIL")
    val picture = column[Option[String]]("PICTURE")

    override def * = (uuid, secId, name, email, picture) // scalastyle:ignore

  }

  class UserSessionTable(
      val tag: Tag
  ) extends Table[UserSession](tag, Some(schema), "USER_SESSION") {

    val uuid = column[SessionUUID]("SESSION_UUID", O.PrimaryKey)
    val token = column[Option[BearerToken]]("TOKEN")
    val userUuid = column[Option[ActorId]]("USER_UUID")
    val loginTime = column[Option[JSqlTimestamp]]("LOGIN_TIME")
    val lastActive = column[Option[JSqlTimestamp]]("LAST_ACTIVE")
    val isLoggedIn = column[Boolean]("IS_LOGGED_IN")
    val tokenExpiry = column[Option[Long]]("TOKEN_EXPIRES_IN")

    val create = (
      uuid: SessionUUID,
      accessToken: Option[BearerToken],
      userId: Option[ActorId],
      loginTimestamp: Option[JSqlTimestamp],
      lastActiveTimestamp: Option[JSqlTimestamp],
      loggedIn: Boolean,
      expiration: Option[Long]
    ) =>
      UserSession(
        uuid = uuid,
        token = accessToken,
        userId = userId,
        loginTime = loginTimestamp,
        lastActive = lastActiveTimestamp,
        isLoggedIn = loggedIn,
        tokenExpiry = expiration
      )

    val destroy = (us: UserSession) =>
      Some((
        us.uuid,
        us.token,
        us.userId,
        us.loginTime,
        us.lastActive,
        us.isLoggedIn,
        us.tokenExpiry
      ))

    override def * = (uuid, token, userUuid, loginTime, lastActive, isLoggedIn, tokenExpiry) <> (create.tupled, destroy) // scalastyle:ignore

  }

  class UserGroupTable(
      val tag: Tag
  ) extends Table[UserGroupMembership](tag, Some(schema), "USER_AUTH_GROUP") {

    val id = column[Option[Int]]("UAG_ID", O.PrimaryKey, O.AutoInc)
    val feideEmail = column[Email]("USER_FEIDE_EMAIL")
    val groupId = column[GroupId]("GROUP_UUID")
    val collectionId = column[Option[CollectionUUID]]("COLLECTION_UUID")

    val create = (
      id: Option[Int],
      feideEmail: Email,
      groupId: GroupId,
      collection: Option[CollectionUUID]
    ) => UserGroupMembership(id, feideEmail, groupId, collection)

    val destroy = (ugm: UserGroupMembership) =>
      Some((ugm.id, ugm.feideEmail, ugm.groupId, ugm.collection))

    override def * = (id, feideEmail, groupId, collectionId) <> (create.tupled, destroy) // scalastyle:ignore
  }

  type GrpColTuples = Seq[(GroupDBTuple, CollectionDBTuple)]

  /**
   * Helper function to locate GroupInfo entries for a query based on the
   * UserGroupTable. The query will be joined with the GroupTable and the
   * MuseumCollectionTable to calculate valid GroupInfo instances.
   *
   * @param q
   * @param ec
   * @return A Future MusitResult containing collection of GroupInfos
   */
  def findGroupInfoBy(
    q: Query[UserGroupTable, UserGroupMembership, Seq]
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[GroupInfo]]] = {
    val q1 = q join grpTable on (_.groupId === _.id)
    val q2 = q1 join musColTable on { (ugg, col) =>
      ugg._1.collectionId === col.uuid || ugg._1.collectionId.isEmpty
    }
    val query = for (((_, grp), col) <- q2) yield (grp, col)

    db.run(query.distinct.result).map(gc => MusitSuccess(foldGroupCol(gc)))
  }

  def convertColGrpTuples(tuples: GrpColTuples): Seq[(GroupInfo, MuseumCollection)] = {
    tuples.map(convertColGrpTuple)
  }

  def convertColGrpTuple(
    tuple: (GroupDBTuple, CollectionDBTuple)
  ): (GroupInfo, MuseumCollection) = {
    (GroupInfo.fromTuple(tuple._1), MuseumCollection.fromTuple(tuple._2))
  }

  /**
   * Function that helps to reduce rows of GroupInfo and MuseumCollections into
   * one GroupInfo (per unique group) containing all its MuseumCollection.
   *
   * @param tuples A Sequence of tuples of GroupTableType and CollectionTableType
   * @return A List of GroupInfo data
   */
  def foldGroupCol(tuples: GrpColTuples): List[GroupInfo] = {
    convertColGrpTuples(tuples).foldLeft(List.empty[GroupInfo]) { (grpInfos, tuple) =>
      val currGrp = tuple._1
      val currCol = tuple._2

      if (grpInfos.exists(_.id == currGrp.id)) {
        grpInfos.map { gi =>
          if (gi.id == currGrp.id) gi.copy(collections = gi.collections :+ currCol)
          else gi
        }
      } else {
        grpInfos :+ currGrp.copy(collections = currGrp.collections :+ currCol)
      }
    }
  }

}
