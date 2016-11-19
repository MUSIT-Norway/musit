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
import models.{Group, GroupAdd}
import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Result
import services.GroupService

import scala.concurrent.Future

@Singleton
class GroupController @Inject() (
    val authService: Authenticator,
    val grpService: GroupService
) extends MusitController {

  val logger = Logger(classOf[GroupController])

  private def serverError(msg: String): Result =
    InternalServerError(Json.obj("message" -> msg))

  def addGroup() = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[GroupAdd] match {
      case JsSuccess(ga, _) =>
        grpService.add(ga).map {
          case MusitSuccess(grp) => Created(Json.toJson(grp))
          case err: MusitError => serverError(err.message)
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  def addUserToGroup(
    groupId: String,
    userId: String
  ) = MusitSecureAction().async { implicit request =>
    val ug = for {
      u <- ActorId.validate(userId)
      g <- GroupId.validate(groupId)
    } yield (ActorId(u), GroupId(g))

    ug.toOption.map {
      case (uid, gid) =>
        grpService.addUserToGroup(uid, gid).map {
          case MusitSuccess(()) => Created
          case err: MusitError => serverError(err.message)
        }
    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for either $groupId or $userId")
        )
      }
    }
  }

  def getGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      grpService.group(gid).map {
        case MusitSuccess(grp) => Ok(Json.toJson(grp))
        case err: MusitError => serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
      }
    }
  }

  def allGroup = MusitSecureAction().async { implicit request =>
    grpService.allGroups.map {
      case MusitSuccess(grps) => if (grps.nonEmpty) Ok(Json.toJson(grps)) else NoContent
      case err: MusitError => serverError(err.message)
    }
  }

  def usersInGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      grpService.listUsersInGroup(gid).map {
        case MusitSuccess(usrs) => if (usrs.nonEmpty) Ok(Json.toJson(usrs)) else NoContent
        case err: MusitError => serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
      }
    }
  }

  def groupsForUser(userId: String) = MusitSecureAction().async { implicit request =>
    ActorId.validate(userId).toOption.map { uid =>
      grpService.listGroupsFor(uid).map {
        case MusitSuccess(grps) => if (grps.nonEmpty) Ok(Json.toJson(grps)) else NoContent
        case err: MusitError => serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $userId"))
      }
    }
  }

  def updateGroup(
    groupId: String
  ) = MusitSecureAction().async(parse.json) { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      request.body.validate[Group] match {
        case JsSuccess(grp, _) =>
          grpService.updateGroup(grp).map {
            case MusitSuccess(mg) =>
              mg.map(g => Ok(Json.toJson(g))).getOrElse {
                Ok(Json.obj("message" -> "Group was not updated"))
              }

            case err: MusitError =>
              serverError(err.message)
          }

        case jsErr: JsError =>
          Future.successful(BadRequest(JsError.toJson(jsErr)))
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
      }
    }
  }

  def removeGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      grpService.removeGroup(gid).map {
        case MusitSuccess(numDel) =>
          val msg = {
            if (numDel == 1) s"Group $groupId was removed"
            else s"Group $groupId was not removed"
          }
          Ok(Json.obj("message" -> msg))

        case err: MusitError =>
          serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
      }
    }
  }

  def removeUserFromGroup(
    groupId: String,
    userId: String
  ) = MusitSecureAction().async { implicit request =>
    val ug = for {
      u <- ActorId.validate(userId)
      g <- GroupId.validate(groupId)
    } yield (ActorId(u), GroupId(g))

    ug.toOption.map {
      case (uid, gid) =>
        grpService.removeUserFromGroup(uid, gid).map {
          case MusitSuccess(numDel) =>
            val msg = {
              if (numDel == 1) s"User $userId was removed from group $groupId"
              else s"User $userId was not removed from group $groupId"
            }
            Ok(Json.obj("message" -> msg))

          case err: MusitError =>
            serverError(err.message)
        }

    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for either $groupId or $userId")
        )
      }
    }
  }

}
