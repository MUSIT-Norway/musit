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

import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{ActorId, Email}

import scala.concurrent.{ExecutionContext, Future}

trait AuthResolver {

  /**
   * Locate the GroupInfos where the provided feide Email is a member.
   *
   * @param email Email with the feide username
   * @param ec    ExecutionContext
   * @return A MusitResult with a Seq of GroupInfo where the user is a member.
   */
  def findGroupInfoByFeideEmail(
    email: Email
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[GroupInfo]]]

  /**
   * Persist the provided UserInfo.
   *
   * @param userInfo UserInfo to save.
   * @param ec       ExecutionContext
   * @return a MusitResult[Unit].
   */
  def saveUserInfo(
    userInfo: UserInfo
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]]

  /**
   * Find the UserInfo data for the given ActorId
   *
   * @param userId ActorId
   * @param ec     ExecutionContext
   * @return MusitResult of an Option of UserInfo
   */
  def getUserInfo(
    userId: ActorId
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserInfo]]]

  /**
   * Prepares a new persistent user session
   */
  def sessionInit()(implicit ec: ExecutionContext): Future[MusitResult[SessionUUID]]

  /**
   * Fetch the UserSession with the given SessionUUID
   *
   * @param sessionUUID SessionUUID to fetch
   * @param ec          ExecutionContext
   * @return MusitResult containing the located UserSession
   */
  def userSession(
    sessionUUID: SessionUUID
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserSession]]]

  /**
   * Save the changes to the provided UserSession.
   *
   * @param userSession the UserSession to save
   * @param ec ExecutionContext
   * @return a MusitResult[Unit]
   */
  def updateSession(
    userSession: UserSession
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]]

}
