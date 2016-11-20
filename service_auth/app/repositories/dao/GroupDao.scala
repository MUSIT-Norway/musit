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
import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class GroupDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends AuthTables {

  import driver.api._

  val logger = Logger(classOf[GroupDao])

  /**
   *
   * @param g
   * @return
   */
  def add(g: GroupAdd): Future[MusitResult[Group]] = {
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
  def findById(grpId: GroupId): Future[MusitResult[Option[Group]]] = {
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
  def findUsersInGroup(grpId: GroupId): Future[MusitResult[Seq[ActorId]]] = {
    val query = usrGrpTable.filter(_.groupId === grpId).map(_.userId)
    db.run(query.result).map { res =>
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
   * @param usrId
   * @return
   */
  def findGroupsFor(usrId: ActorId): Future[MusitResult[Seq[Group]]] = {
    val ugQuery = usrGrpTable.filter(_.userId === usrId)
    val query = for {
      (ug, g) <- ugQuery join grpTable on (_.groupId === _.id)
    } yield g

    db.run(query.result).map { grps =>
      MusitSuccess(grps.map {
        case (gid, name, perm, mid, desc) => Group(gid, name, perm, mid, desc)
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to find Groups for user $usrId"
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
  def update(grp: Group): Future[MusitResult[Option[Group]]] = {
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
  def delete(grpId: GroupId): Future[MusitResult[Int]] = {
    val action = grpTable.filter(_.id === grpId).delete
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
   * @param usrId
   * @param grpId
   * @return
   */
  def addUserToGroup(usrId: ActorId, grpId: GroupId): Future[MusitResult[Unit]] = {
    val action = usrGrpTable += ((usrId, grpId))

    db.run(action).map(res => MusitSuccess(())).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when inserting a new UserGroup relation " +
          s"between userId $usrId and groupId $grpId"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))

    }
  }

  /**
   *
   * @param usrId
   * @param grpId
   * @return
   */
  def removeUserFromGroup(
    usrId: ActorId,
    grpId: GroupId
  ): Future[MusitResult[Int]] = {
    val action = usrGrpTable.filter { ug =>
      ug.userId === usrId && ug.groupId === grpId
    }.delete

    db.run(action).map {
      case res: Int if res == 1 =>
        logger.debug(s"Successfully removed UserGroup ($usrId, $grpId)")
        MusitSuccess(res)

      case res: Int if res == 0 =>
        logger.debug(s"UserGroup ($usrId, $grpId) was not removed.")
        MusitSuccess(res)

      case res: Int =>
        val msg = s"An unexpected amount of UserGroups where removed using " +
          s"UserGroup ($usrId, $grpId)"
        logger.warn(msg)
        MusitDbError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"There was an error deleting the UserGroup ($usrId, $grpId)"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

}
