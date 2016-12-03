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

import com.google.inject.{Inject, Singleton}
import models.{Group, GroupAdd}
import no.uio.musit.models.{CollectionUUID, GroupId, MuseumCollection, UserGroupMembership} // scalastyle:ignore
import no.uio.musit.security.{AuthTables, GroupInfo}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AuthDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends AuthTables {

  import driver.api._

  val logger = Logger(classOf[AuthDao])

  /**
   *
   * @param g
   * @return
   */
  def addGroup(g: GroupAdd): Future[MusitResult[Group]] = {
    val gid = GroupId.generate()
    val action = grpTable += ((gid, g.name, g.permission, g.museumId, g.description))

    db.run(action).map(res => MusitSuccess(Group.fromGroupAdd(gid, g))).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when inserting new Group ${g.name}"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param grpId
   * @return
   */
  def findGroupById(grpId: GroupId): Future[MusitResult[Option[Group]]] = {
    val query = grpTable.filter(_.id === grpId).result.headOption
    db.run(query).map { res =>
      MusitSuccess(res.map {
        case (gid, name, perm, mid, desc) => Group(gid, name, perm, mid, desc)
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to find Group $grpId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param grpId
   * @return
   */
  def findUsersInGroup(grpId: GroupId): Future[MusitResult[Seq[String]]] = {
    val q = usrGrpTable.filter(_.groupId === grpId).map(_.feideEmail).distinct
    db.run(q.result).map { res =>
      MusitSuccess(res)
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to find users in Group $grpId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param feideEmail
   * @return
   */
  def findGroupInfoFor(feideEmail: String): Future[MusitResult[Seq[GroupInfo]]] = {
    findGroupInfoBy(usrGrpTable.filter(_.feideEmail === feideEmail)).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to find GroupInfo for user $feideEmail"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param feideEmail
   * @param groupId
   * @return
   */
  def findCollectionsFor(
    feideEmail: String,
    groupId: GroupId
  ): Future[MusitResult[Seq[MuseumCollection]]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail === feideEmail && ug.groupId === groupId
    }
    val query = for {
      (ug, c) <- q join musColTable on (_.collectionId === _.uuid)
    } yield c

    db.run(query.result).map { cols =>
      MusitSuccess(cols.map {
        case (id, name, schemas) => MuseumCollection(id, name, schemas)
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to get collections " +
          s"for $feideEmail in group $groupId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param feideEmail
   * @param groupId
   * @param collectionId
   * @return
   */
  def revokeCollectionFor(
    feideEmail: String,
    groupId: GroupId,
    collectionId: CollectionUUID
  ): Future[MusitResult[Int]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail === feideEmail &&
        ug.groupId === groupId &&
        ug.collectionId === collectionId
    }.delete

    db.run(q).map {
      case res: Int if res == 1 =>
        logger.debug(s"Successfully revoked collection $collectionId " +
          s"for $feideEmail on $groupId")
        MusitSuccess(res)

      case res: Int if res == 0 =>
        logger.debug(s"Collection $collectionId was not removed from $feideEmail " +
          s"on $groupId.")
        MusitSuccess(res)

      case res: Int =>
        val msg = s"An unexpected amount of collections where removed from " +
          s"GroupId $groupId for $feideEmail"
        logger.warn(msg)
        MusitDbError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to delete the " +
          s"collection $collectionId for $feideEmail from $groupId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   * WARNING: This is a full table scan
   */
  def allGroups: Future[MusitResult[Seq[Group]]] = {
    val query = grpTable.sortBy(_.name)
    db.run(query.result).map { grps =>
      MusitSuccess(grps.map {
        case (gid, name, perm, mid, desc) => Group(gid, name, perm, mid, desc)
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to get all Groups"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param grp
   * @return
   */
  def updateGroup(grp: Group): Future[MusitResult[Option[Group]]] = {
    val action = grpTable.filter { g =>
      g.id === grp.id
    }.update((grp.id, grp.name, grp.permission, grp.museumId, grp.description))

    db.run(action).map {
      case res: Int if res == 1 => MusitSuccess(Some(grp))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = s"Wrong amount of rows ($res) updated for group ${grp.id}"
        logger.warn(msg)
        MusitDbError(msg)
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to update the Group ${grp.id}"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param grpId
   * @return
   */
  def deleteGroup(grpId: GroupId): Future[MusitResult[Int]] = {
    val action1 = usrGrpTable.filter(_.groupId === grpId).delete
    val action2 = grpTable.filter(_.id === grpId).delete

    // First we need to remove the users from the group.
    // Then we remove the group.
    val action = action1.andThen(action2).transactionally

    db.run(action).map {
      case res: Int if res == 1 =>
        logger.debug(s"Successfully removed Group $grpId")
        MusitSuccess(res)

      case res: Int if res == 0 =>
        logger.debug(s"Group $grpId was not removed.")
        MusitSuccess(res)

      case res: Int =>
        val msg = s"An unexpected amount of groups where removed using GroupId $grpId"
        logger.warn(msg)
        MusitDbError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to delete the Group $grpId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param feideEmail
   * @param grpId
   * @param maybeCollections
   * @return
   */
  def addUserToGroup(
    feideEmail: String,
    grpId: GroupId,
    maybeCollections: Option[Seq[CollectionUUID]]
  ): Future[MusitResult[Unit]] = {
    val ugms = UserGroupMembership.applyMulti(feideEmail, grpId, maybeCollections)

    val action = (usrGrpTable ++= ugms).map(_.fold(1)(identity))

    db.run(action).map { res =>
      logger.debug(s"Added user $feideEmail to group $grpId returned ${res}")
      MusitSuccess(())
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when inserting a new UserGroup relation " +
          s"between user $feideEmail and groupId $grpId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @param feideEmail
   * @param grpId
   * @return
   */
  def removeUserFromGroup(
    feideEmail: String,
    grpId: GroupId
  ): Future[MusitResult[Int]] = {
    val action = usrGrpTable.filter { ug =>
      ug.feideEmail === feideEmail && ug.groupId === grpId
    }.delete

    db.run(action).map {
      case res: Int if res == 1 =>
        logger.debug(s"Successfully removed UserGroup ($feideEmail, $grpId)")
        MusitSuccess(res)

      case res: Int if res == 0 =>
        logger.debug(s"UserGroup ($feideEmail, $grpId) was not removed.")
        MusitSuccess(res)

      case res: Int =>
        val msg = s"An unexpected amount of UserGroups where removed using " +
          s"UserGroup ($feideEmail, $grpId)"
        logger.warn(msg)
        MusitDbError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"There was an error deleting the UserGroup ($feideEmail, $grpId)"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   *
   * @return
   */
  def allCollections: Future[MusitResult[Seq[MuseumCollection]]] = {
    val query = musColTable.sortBy(_.name)
    db.run(query.result).map { cols =>
      MusitSuccess(cols.map {
        case (uuid, name, oldSchemas) => MuseumCollection(uuid, name, oldSchemas)
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to get all MuseumCollections."
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }
}
