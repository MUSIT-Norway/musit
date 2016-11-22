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

import no.uio.musit.models.ActorId
import no.uio.musit.models.Museums.Museum
import no.uio.musit.security.Permissions.{ElevatedPermission, GodMode, Permission}
import no.uio.musit.service.MusitResults.{MusitNotAuthorized, MusitResult, MusitSuccess}
import play.api.Logger

case class AuthenticatedUser(userInfo: UserInfo, groups: Seq[GroupInfo]) {

  val id: ActorId = userInfo.id

  private val logger = Logger(classOf[AuthenticatedUser])

  private def lowestPermission(permissions: Seq[Permission]): Permission = {
    if (permissions.nonEmpty) {
      permissions.reduceLeft((a, b) => if (a.priority > b.priority) b else a)
    } else {
      Permissions.Unspecified
    }
  }

  private def hasGodMode: Boolean = groups.exists(_.hasPermission(GodMode))

  private def permissionsFor(museum: Museum): Seq[Permission] =
    groups.filter(_.museumId == museum.id).map(gi => gi.permission)

  private def isAuthorizedFor(museum: Museum): Boolean = {
    groups.exists(_.museum.contains(museum))
  }

  private def authorizeUser(
    museum: Museum,
    permissions: Seq[Permission]
  ): MusitResult[Unit] = {
    val lowest = lowestPermission(permissions)

    if (hasGodMode) {
      logger.debug(s"User with GodMode accessing system.")
      MusitSuccess(())
    } else if (isAuthorizedFor(museum)) {
      val allowed = permissionsFor(museum).exists(_.priority >= lowest.priority)
      if (allowed) MusitSuccess(())
      else MusitNotAuthorized()
    } else {
      MusitNotAuthorized()
    }
  }

  def authorize(
    museum: Museum,
    permissions: Seq[Permission]
  ): MusitResult[Unit] = authorizeUser(museum, permissions)

  def authorizeAdmin(
    maybeMuseum: Option[Museum],
    permissions: Seq[ElevatedPermission]
  ): MusitResult[Unit] = {
    maybeMuseum.map { museum =>
      authorizeUser(museum, permissions)
    }.getOrElse {
      // Only GodMode has access to all museums
      if (hasGodMode) MusitSuccess(())
      else MusitNotAuthorized()
    }
  }

}
