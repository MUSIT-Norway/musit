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

package controllers.rest

import com.google.inject.{Inject, Singleton}
import models.{GroupAdd, GroupId}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.GroupService

import scala.concurrent.Future

@Singleton
class GroupController @Inject() (
    val authService: Authenticator,
    val grpService: GroupService
) extends MusitController {

  val logger = Logger(classOf[GroupController])

  def addGroup() = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[GroupAdd] match {
      case JsSuccess(ga, _) =>
        grpService.add(ga).map {
          case MusitSuccess(grp) =>
            Ok(Json.toJson(grp))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  def addUserToGroup(
    userId: String,
    groupId: String
  ) = MusitSecureAction().async { implicit request =>
    ???
  }

  def getGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    ???
  }

  def allGroup = MusitSecureAction().async { implicit request =>
    ???
  }

  def usersInGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    ???
  }

  def groupsForUser(userId: String) = MusitSecureAction().async { implicit request =>
    ???
  }

  def updateGroup(
    groupId: GroupId
  ) = MusitSecureAction().async(parse.json) { implicit request =>
    ???
  }

  def removeGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    ???
  }

  def removeUserFromGroup(
    userId: String,
    groupId: String
  ) = MusitSecureAction().async { implicit request =>
    ???
  }

}
