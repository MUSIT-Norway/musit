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

package services

import com.google.inject.Inject
import models.{Group, GroupAdd}
import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.service.MusitResults.MusitResult
import play.api.Logger
import repositories.dao.GroupDao

import scala.concurrent.Future

class GroupService @Inject() (val dao: GroupDao) {

  val logger = Logger(classOf[GroupService])

  type MusitResultF[A] = Future[MusitResult[A]]

  /**
   *
   * @param grpAdd
   * @return
   */
  def add(grpAdd: GroupAdd): MusitResultF[Group] = dao.add(grpAdd)

  /**
   *
   * @param email
   * @param grpId
   * @param uid
   * @return
   */
  def addUserToGroup(
    email: String,
    grpId: GroupId,
    uid: Option[ActorId] = None
  ): MusitResultF[Unit] = dao.addUserToGroup(email, grpId, uid)

  /**
   *
   * @param grpId
   * @return
   */
  def group(grpId: GroupId): MusitResultF[Option[Group]] = dao.findById(grpId)

  /**
   *
   * @return
   */
  def allGroups: MusitResultF[Seq[Group]] = dao.allGroups

  /**
   *
   * @param grpId
   * @return
   */
  def listUsersInGroup(grpId: GroupId): MusitResultF[Seq[String]] =
    dao.findUsersInGroup(grpId)

  /**
   *
   * @param email
   * @return
   */
  def listGroupsFor(email: String): MusitResultF[Seq[Group]] = dao.findGroupsFor(email)

  /**
   *
   * @param grp
   * @return
   */
  def updateGroup(grp: Group): MusitResultF[Option[Group]] = dao.update(grp)

  /**
   *
   * @param grpId
   * @return
   */
  def removeGroup(grpId: GroupId): MusitResultF[Int] = dao.delete(grpId)

  /**
   *
   * @param email
   * @param grpId
   * @return
   */
  def removeUserFromGroup(email: String, grpId: GroupId): MusitResultF[Int] =
    dao.removeUserFromGroup(email, grpId)
}
