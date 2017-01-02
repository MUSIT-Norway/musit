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
import models.{Group, GroupAdd, UserAuthAdd}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.{Email, GroupId}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.MusitAdmin
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.Result
import repositories.dao.AuthDao

import scala.concurrent.Future

@Singleton
class GroupController @Inject() (
    implicit
    val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  val logger = Logger(classOf[GroupController])

  private def serverError(msg: String): Result =
    InternalServerError(Json.obj("message" -> msg))

  def addGroup(
    mid: Int
  ) = MusitAdminAction(mid, MusitAdmin).async(parse.json) { implicit request =>
    request.body.validate[GroupAdd] match {
      case JsSuccess(ga, _) =>
        dao.addGroup(ga).map {
          case MusitSuccess(grp) => Created(Json.toJson(grp))
          case err: MusitError => serverError(err.message)
        }

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  def addUserToGroup(
    groupId: String
  ) = MusitSecureAction(MusitAdmin).async(parse.json) { implicit request =>

    request.body.validate[UserAuthAdd] match {
      case JsSuccess(uad, _) =>
        Email.fromString(uad.email).map { feideEmail =>
          GroupId.validate(groupId).toOption.map(GroupId.apply).map { gid =>
            dao.addUserToGroup(feideEmail, gid, uad.collections).map {
              case MusitSuccess(()) => Created
              case err: MusitError => serverError(err.message)
            }
          }.getOrElse {
            Future.successful {
              BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
            }
          }
        }.getOrElse {
          Future.successful {
            BadRequest(Json.obj("message" -> s"Invalid email ${uad.email}"))
          }
        }

      case jserr: JsError =>
        Future.successful(BadRequest(JsError.toJson(jserr)))
    }
  }

  def getGroup(groupId: String) = MusitSecureAction().async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      dao.findGroupById(gid).map {
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
    dao.allGroups.map {
      case MusitSuccess(grps) => if (grps.nonEmpty) Ok(Json.toJson(grps)) else NoContent
      case err: MusitError => serverError(err.message)
    }
  }

  def usersInGroup(
    groupId: String
  ) = MusitAdminAction(MusitAdmin).async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      dao.findUsersInGroup(gid).map {
        case MusitSuccess(usrs) =>
          if (usrs.nonEmpty) Ok(Json.toJson(usrs)) else NoContent

        case err: MusitError =>
          serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid UUID for $groupId"))
      }
    }
  }

  def groupsForUser(
    email: String
  ) = MusitSecureAction().async { implicit request =>
    Email.fromString(email).map { feideEmail =>
      dao.findGroupInfoFor(feideEmail).map {
        case MusitSuccess(grps) => if (grps.nonEmpty) Ok(Json.toJson(grps)) else NoContent
        case err: MusitError => serverError(err.message)
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid email $email"))
      }
    }
  }

  def updateGroup(
    groupId: String
  ) = MusitAdminAction(MusitAdmin).async(parse.json) { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      request.body.validate[Group] match {
        case JsSuccess(grp, _) =>
          dao.updateGroup(grp).map {
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

  def removeGroup(
    groupId: String
  ) = MusitAdminAction(MusitAdmin).async { implicit request =>
    GroupId.validate(groupId).toOption.map { gid =>
      dao.deleteGroup(gid).map {
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
    email: String
  ) = MusitAdminAction(MusitAdmin).async { implicit request =>
    Email.fromString(email).map { feideEmail =>
      GroupId.validate(groupId).toOption.map(GroupId.apply).map { gid =>
        dao.removeUserFromGroup(feideEmail, gid).map {
          case MusitSuccess(numDel) =>
            val msg = {
              if (numDel == 1) s"User $email was removed from group $groupId"
              else s"User $email was not removed from group $groupId"
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
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"Invalid email $email"))
      }
    }
  }

}
