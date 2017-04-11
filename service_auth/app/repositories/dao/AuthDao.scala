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
import no.uio.musit.models._
import no.uio.musit.security.{AuthTables, GroupInfo, UserInfo}
import no.uio.musit.MusitResults.{MusitDbError, MusitError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AuthDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AuthTables {

  import profile.api._

  val logger = Logger(classOf[AuthDao])

  private def handleError(msg: String, ex: Throwable): MusitError = {
    logger.error(msg, ex)
    MusitDbError(msg, Some(ex))
  }

  /**
   * method for adding a Group to the AUTH_GROUP table.
   *
   * @param g GroupAdd data to insert
   * @return The newly inserted Group complete with a GroupId.
   */
  def addGroup(g: GroupAdd): Future[MusitResult[Group]] = {
    val gid    = GroupId.generate()
    val action = grpTable +=
      ((gid, g.name, g.module, g.permission, g.museumId, g.description))

    db.run(action).map(_ => MusitSuccess(Group.fromGroupAdd(gid, g))).recover {
      case NonFatal(ex) =>
        handleError(s"An error occurred inserting new Group ${g.name}", ex)
    }
  }

  /**
   * find a Group based on GroupId
   *
   * @param grpId the GroupId to search for
   * @return An Option of Group
   */
  def findGroupById(grpId: GroupId): Future[MusitResult[Option[Group]]] = {
    val query = grpTable.filter(_.id === grpId).result.headOption
    db.run(query)
      .map { res =>
        MusitSuccess(res.map {
          case (gid, name, mod, perm, mid, desc) => Group(gid, name, mod, perm, mid, desc)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred trying to find Group $grpId", ex)
      }
  }

  /**
   * find all the users feide-emails in a Group based on GroupId
   *
   * @param grpId GroupId to look for user in
   * @return A collection of feide Emails.
   */
  def findUsersInGroup(grpId: GroupId): Future[MusitResult[Seq[Email]]] = {
    val q = usrGrpTable.filter(_.groupId === grpId).map(_.feideEmail).distinct
    db.run(q.result)
      .map { res =>
        MusitSuccess(res)
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred trying to find users in Group $grpId", ex)
      }
  }

  /**
   * find GroupInfos for the given feide-email.
   *
   * @param feideEmail The feide email
   * @return A collection of GroupInfo belonging to the given feide-email
   */
  def findGroupInfoFor(feideEmail: Email): Future[MusitResult[Seq[GroupInfo]]] = {
    findGroupInfoBy(usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail
    }).recover {
      case NonFatal(ex) =>
        handleError(s"An error occurred trying find GroupInfo for user $feideEmail", ex)
    }
  }

  /**
   *
   * @param feideEmail
   * @param groupId
   * @return
   */
  def findCollectionsFor(
      feideEmail: Email,
      groupId: GroupId
  ): Future[MusitResult[Seq[MuseumCollection]]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail && ug.groupId === groupId
    }
    val query = for {
      (_, c) <- q join musColTable on (_.collectionId === _.uuid)
    } yield c

    db.run(query.result)
      .map { cols =>
        MusitSuccess(cols.map {
          case (id, name, schemas) => MuseumCollection(id, name, schemas)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred trying to get collections " +
              s"for $feideEmail in group $groupId",
            ex
          )
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
      feideEmail: Email,
      groupId: GroupId,
      collectionId: CollectionUUID
  ): Future[MusitResult[Int]] = {
    val q = usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail &&
      ug.groupId === groupId &&
      ug.collectionId === collectionId
    }.delete

    db.run(q)
      .map {
        case res: Int if res == 1 =>
          logger.debug(
            s"Successfully revoked collection $collectionId " +
              s"for $feideEmail on $groupId"
          )
          MusitSuccess(res)

        case res: Int if res == 0 =>
          logger.debug(
            s"Collection $collectionId was not removed from $feideEmail " +
              s"on $groupId."
          )
          MusitSuccess(res)

        case res: Int =>
          val msg = s"An unexpected amount of collections where removed from " +
            s"GroupId $groupId for $feideEmail"
          logger.warn(msg)
          MusitDbError(msg)

      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred when trying to delete the " +
              s"collection $collectionId for $feideEmail from $groupId",
            ex
          )
      }
  }

  /**
   * WARNING: This is a full table scan
   */
  def allGroups: Future[MusitResult[Seq[Group]]] = {
    val query = grpTable.sortBy(_.name)
    db.run(query.result)
      .map { grps =>
        MusitSuccess(grps.map {
          case (gid, name, mod, perm, mid, desc) => Group(gid, name, mod, perm, mid, desc)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to get all Groups", ex)
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
    }.update((grp.id, grp.name, grp.module, grp.permission, grp.museumId, grp.description))

    db.run(action)
      .map {
        case res: Int if res == 1 => MusitSuccess(Some(grp))
        case res: Int if res == 0 => MusitSuccess(None)
        case res: Int =>
          val msg = s"Wrong amount of rows ($res) updated for group ${grp.id}"
          logger.warn(msg)
          MusitDbError(msg)
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to update the Group ${grp.id}", ex)
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

    db.run(action)
      .map {
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

      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to delete the Group $grpId", ex)
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
      feideEmail: Email,
      grpId: GroupId,
      maybeCollections: Option[Seq[CollectionUUID]]
  ): Future[MusitResult[Unit]] = {
    val ugms = UserGroupMembership.applyMulti(feideEmail, grpId, maybeCollections)

    val action = (usrGrpTable ++= ugms).map(_.fold(1)(identity))

    db.run(action)
      .map { res =>
        logger.debug(s"Added user $feideEmail to group $grpId returned $res")
        MusitSuccess(())
      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"An error occurred when inserting a new UserGroup relation " +
              s"between user $feideEmail and groupId $grpId",
            ex
          )
      }
  }

  def findUserGroupMembership(
      grpId: GroupId,
      email: Email
  ): Future[MusitResult[Seq[UserGroupMembership]]] = {
    val query = usrGrpTable.filter { ug =>
      ug.groupId === grpId && ug.feideEmail === email
    }.result

    db.run(query)
      .map { res =>
        MusitSuccess(res)
      }
      .recover {
        case NonFatal(ex) =>
          val msg = s"An error occurred when fetching user group memberships" +
            s"for $email in group $grpId"
          handleError(msg, ex)
      }
  }

  /**
   *
   * @param feideEmail
   * @param grpId
   * @return
   */
  def removeUserFromGroup(
      feideEmail: Email,
      grpId: GroupId
  ): Future[MusitResult[Int]] = {
    val action = usrGrpTable.filter { ug =>
      ug.feideEmail.toLowerCase === feideEmail && ug.groupId === grpId
    }.delete

    db.run(action)
      .map {
        case res: Int if res == 0 =>
          logger.debug(s"UserGroup ($feideEmail, $grpId) was not removed.")
          MusitSuccess(res)

        case res: Int =>
          logger.debug(s"Successfully removed UserGroup ($feideEmail, $grpId)")
          MusitSuccess(res)

      }
      .recover {
        case NonFatal(ex) =>
          handleError(
            s"There was an error deleting the UserGroup " +
              s"($feideEmail, $grpId)",
            ex
          )
      }
  }

  /**
   *
   * @return
   */
  def allCollections: Future[MusitResult[Seq[MuseumCollection]]] = {
    val query = musColTable.sortBy(_.name)
    db.run(query.result)
      .map { cols =>
        MusitSuccess(cols.map {
          case (uuid, name, oldSchemas) => MuseumCollection(uuid, name, oldSchemas)
        })
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred when trying to get all MuseumCollections.", ex)
      }
  }

  def allUsers: Future[MusitResult[Seq[UserInfo]]] = {
    val query = usrInfoTable.sortBy(_.name.nullsLast)
    db.run(query.result)
      .map { res =>
        MusitSuccess(
          res.map { u =>
            UserInfo(
              id = u._1,
              secondaryIds = u._2.map(fe => Seq(fe.value)),
              name = u._3,
              email = u._4,
              picture = u._5
            )
          }
        )
      }
      .recover {
        case NonFatal(ex) =>
          handleError(s"An error occurred trying to fetch registered users.", ex)
      }
  }
}
