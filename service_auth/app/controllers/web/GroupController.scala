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

package controllers.web

import com.google.inject.Inject
import models.GroupAdd._
import models.UserAuthAdd._
import models.Group
import no.uio.musit.models.{CollectionUUID, GroupId}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitResult, MusitSuccess}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import repositories.dao.AuthDao

import scala.concurrent.Future
import scala.util.control.NonFatal

class GroupController @Inject() (
    implicit
    val authService: Authenticator,
    val dao: AuthDao,
    val messagesApi: MessagesApi,
    val ws: WSClient,
    val configuration: Configuration
) extends MusitController with I18nSupport {

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  private def handleNotFound(msg: String): Future[Result] = {
    Future.successful(NotFound(views.html.error(msg)))
  }

  private def handleBadRequest(msg: String): Future[Result] = {
    Future.successful(NotFound(views.html.error(msg)))
  }

  /**
   *
   * @param mid
   * @param gid
   * @return
   */
  def deleteGroup(mid: Int, gid: String) = Action.async { implicit request =>
    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      dao.deleteGroup(groupId).map {
        case MusitSuccess(int) =>
          Redirect(controllers.web.routes.GroupController.groupList(mid))
            .flashing("success" -> "Group was removed")
        case error: MusitError =>
          BadRequest(
            Json.obj("error" -> error.message)
          )
      }
    }.getOrElse(
      Future.successful(
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for $gid")
        )
      )
    )
  }

  /**
   *
   * @param mid
   * @param email
   * @param gid
   * @return
   */
  def deleteUser(
    mid: Int,
    email: String,
    gid: String
  ) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map(GroupId.apply).map { gid =>
      dao.removeUserFromGroup(email, gid).map {
        case MusitSuccess(int) =>
          Redirect(
            controllers.web.routes.GroupController.groupActorsList(mid, gid.asString)
          ).flashing("success" -> "User was removed")
        case error: MusitError =>
          BadRequest(
            Json.obj("error" -> error.message)
          )
      }
    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for $gid")
        )
      }
    }
  }

  /**
   *
   * @param mid
   * @param gid
   * @return
   */
  def groupAddUserGet(mid: Int, gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map { groupId =>
      dao.allCollections.flatMap { cols =>
        dao.findGroupById(groupId).map {
          case MusitSuccess(group) =>
            group.map { g =>
              Ok(views.html.groupUserAdd(userAuthAddForm, g, cols.getOrElse(Seq.empty)))
            }.getOrElse(BadRequest(views.html.error(s"GroupId $gid was not found")))
          case err: MusitError =>
            BadRequest(views.html.error(s"An error occurred trying to fetch group $gid"))
        }
      }
    }.getOrElse {
      handleBadRequest(s"Invalid groupId $gid")
    }
  }

  /**
   *
   * @param mid
   * @param gid
   * @return
   */
  def groupAddUserPost(mid: Int, gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map { groupId =>
      userAuthAddForm.bindFromRequest.fold(
        formWithErrors => {
          for {
            cres <- dao.allCollections
            gres <- dao.findGroupById(groupId)
          } yield {
            gres.map { maybeGroup =>
              maybeGroup.map { group =>
                BadRequest(views.html.groupUserAdd(
                  theForm = formWithErrors,
                  group = group,
                  collections = cres.getOrElse(Seq.empty)
                ))
              }.getOrElse {
                BadRequest(views.html.error(s"Group with ID $gid was not found"))
              }
            }.getOrElse {
              BadRequest(
                views.html.error(s"An error occurred trying to fetch group $gid")
              )
            }
          }
        },
        userAdd => {
          dao.addUserToGroup(userAdd.email, groupId, userAdd.collections).map {
            case MusitSuccess(group) =>
              Redirect(
                controllers.web.routes.GroupController.groupActorsList(mid, gid)
              ).flashing("success" -> "User added!")
            case error: MusitError =>
              BadRequest(
                Json.obj("error" -> error.message)
              )
          }
        }
      )
    }.getOrElse(
      handleBadRequest(s"Invalid groupId $gid")
    )
  }

  /**
   *
   * @param mid
   * @return
   */
  def groupAddGet(mid: Int) = Action { implicit request =>
    Ok(views.html.groupAdd(groupAddForm, mid, allowedGroups))
  }

  /**
   *
   * @param mid
   * @return
   */
  def groupAddPost(mid: Int) = Action.async { implicit request =>
    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupAdd(formWithErrors, mid, allowedGroups))
        )
      },
      groupAdd => {
        dao.addGroup(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(
              controllers.web.routes.GroupController.groupList(mid)
            ).flashing("success" -> "Group added!")
          case error: MusitError =>
            BadRequest(
              Json.obj("error" -> error.message)
            )
        }
      }
    )
  }

  /**
   *
   * @param mid
   * @return
   */
  def groupList(mid: Int) = Action.async { implicit request =>
    dao.allGroups.map {
      case MusitSuccess(groups) =>
        Ok(views.html.groupList(groups, mid, None))
      case error: MusitError =>
        Ok(views.html.groupList(Seq.empty, mid, Some(error)))
    }
  }

  /**
   *
   * @param mid
   * @param groupId
   * @param groupRes
   * @param usersRes
   * @return
   */
  private def getActorDetailsFor(
    mid: Int,
    groupId: GroupId,
    groupRes: MusitResult[Option[Group]],
    usersRes: MusitResult[Seq[String]]
  ): Future[Result] = {
    groupRes.flatMap { group =>
      usersRes.map { users =>
        Future.sequence {
          // Fetch the GroupInfo for each user
          users.map { usr =>
            dao.findCollectionsFor(usr, groupId).map {
              case MusitSuccess(cols) => (usr, cols)
              case _ => (usr, Seq.empty)
            }
          }
        }.map { ugis =>
          // TODO: We should call getActors(users) if we have an ActorId
          group.map { grp =>
            Ok(views.html.groupActors(ugis, mid, grp))
          }.getOrElse {
            NotFound(views.html.error(s"Could not find group"))
          }
        }
      }
    }.getOrElse {
      Future.successful(
        InternalServerError(views.html.error("An error occurred fetching the group"))
      )
    }
  }

  /**
   *
   * @param mid
   * @param gid
   * @return
   */
  def groupActorsList(mid: Int, gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map(GroupId.apply).map { groupId =>
      val futureRes = for {
        groupRes <- dao.findGroupById(groupId)
        usersRes <- dao.findUsersInGroup(groupId)
        res <- getActorDetailsFor(mid, groupId, groupRes, usersRes)
      } yield res

      futureRes.recover {
        case NonFatal(ex) =>
          InternalServerError(views.html.error("An error occurred fetching data"))
      }
    }.getOrElse {
      handleNotFound(s"Wrong uuid format: $gid")
    }
  }

  /**
   *
   * @param mid
   * @param email
   * @param gid
   * @param cid
   * @return
   */
  def revokeCollectionAuth(
    mid: Int,
    email: String,
    gid: String,
    cid: String
  ) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map(GroupId.apply).map { groupId =>
      CollectionUUID.validate(cid).toOption.map(CollectionUUID.apply).map { colId =>
        dao.revokeCollectionFor(email, groupId, colId).map {
          case MusitSuccess(res) =>
            Redirect(
              controllers.web.routes.GroupController.groupActorsList(mid, gid)
            ).flashing("success" -> "Collection access revoked")
          case err: MusitError =>
            InternalServerError(views.html.error(err.message))
        }
      }.getOrElse {
        handleNotFound(s"Wrong uuid format: $cid")
      }
    }.getOrElse {
      handleNotFound(s"Wrong uuid format: $gid")
    }
  }

}
