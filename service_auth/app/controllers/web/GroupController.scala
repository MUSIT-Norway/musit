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
import models.Group
import models.GroupAdd._
import models.UserAuthAdd._
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.models.{CollectionUUID, Email, GroupId, UserGroupMembership}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitController
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import repositories.dao.AuthDao

import scala.concurrent.Future
import scala.util.control.NonFatal

class GroupController @Inject() (
    val authService: Authenticator,
    val dao: AuthDao,
    val messagesApi: MessagesApi,
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
   * @param gid
   * @return
   */
  def deleteGroup(gid: String) = Action.async { implicit request =>
    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      dao.deleteGroup(groupId).map {
        case MusitSuccess(int) =>
          Redirect(controllers.web.routes.GroupController.groupList())
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
   * @param email
   * @param gid
   * @return
   */
  def deleteUser(
    gid: String,
    email: String
  ) = Action.async { implicit request =>
    Email.fromString(email).map { feideEmail =>
      GroupId.validate(gid).toOption.map(GroupId.apply).map { gid =>
        dao.removeUserFromGroup(feideEmail, gid).map {
          case MusitSuccess(int) =>
            Redirect(
              controllers.web.routes.GroupController.groupUserList(gid.asString)
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
    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid email: $email")
        )
      }
    }
  }

  /**
   *
   * @param gid
   * @return
   */
  def groupAddUserGet(gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map { groupId =>
      dao.allCollections.flatMap { cols =>
        dao.findGroupById(groupId).map {
          case MusitSuccess(group) =>
            group.map { g =>
              Ok(views.html.groupUserAdd(userAuthForm, g, cols.getOrElse(Seq.empty)))
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
   * @param gid
   * @return
   */
  def groupAddUserPost(gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map { groupId =>
      userAuthForm.bindFromRequest.fold(
        formWithErrors => {
          (for {
            cols <- MusitResultT(dao.allCollections)
            maybeGroup <- MusitResultT(dao.findGroupById(groupId))
          } yield {
            maybeGroup.map { group =>
              BadRequest(views.html.groupUserAdd(
                theForm = formWithErrors,
                group = group,
                collections = cols
              ))
            }.getOrElse {
              BadRequest(views.html.error(s"Group with ID $gid was not found"))
            }
          }).value.map(_.getOrElse {
            BadRequest(
              views.html.error(s"An error occurred trying to fetch group $gid")
            )
          })
        },
        userAdd => {
          dao.addUserToGroup(Email(userAdd.email), groupId, userAdd.collections).map {
            case MusitSuccess(group) =>
              Redirect(
                controllers.web.routes.GroupController.groupUserList(gid)
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

  def groupEditUser(gid: String, email: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map { groupId =>
      val feideMail = Email(email)
      (for {
        cols <- MusitResultT(dao.allCollections)
        mgroup <- MusitResultT(dao.findGroupById(groupId))
        mems <- MusitResultT(dao.findUserGroupMembership(groupId, feideMail))
      } yield {
        mgroup.map { group =>
          Ok(views.html.groupUserEdit(group, feideMail, mems, cols, None))
        }.getOrElse {
          BadRequest(views.html.error(s"Group with ID $gid was not found"))
        }
      }).value.map(_.getOrElse {
        BadRequest(
          views.html.error(s"An error occurred trying to fetch user membership" +
            s" for $email in group $gid")
        )
      })
    }.getOrElse(
      handleBadRequest(s"Invalid groupId $gid")
    )
  }

  /**
   *
   * @return
   */
  def groupAddGet = Action { implicit request =>
    Ok(views.html.groupAdd(groupAddForm, allowedGroups))
  }

  /**
   *
   * @return
   */
  def groupAddPost = Action.async { implicit request =>
    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupAdd(formWithErrors, allowedGroups))
        )
      },
      groupAdd => {
        dao.addGroup(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(
              controllers.web.routes.GroupController.groupList()
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
   * @return
   */
  def groupList = Action.async { implicit request =>
    dao.allGroups.map {
      case MusitSuccess(groups) =>
        Ok(views.html.groupList(groups, None))
      case error: MusitError =>
        Ok(views.html.groupList(Seq.empty, Some(error)))
    }
  }

  /**
   *
   * @param groupId
   * @param groupRes
   * @param usersRes
   * @return
   */
  private def getUserDetailsFor(
    groupId: GroupId,
    groupRes: MusitResult[Option[Group]],
    usersRes: MusitResult[Seq[Email]]
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
          group.map { grp =>
            Ok(views.html.groupUsers(ugis, grp))
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
   * @param gid
   * @return
   */
  def groupUserList(gid: String) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map(GroupId.apply).map { groupId =>
      val futureRes = for {
        groupRes <- dao.findGroupById(groupId)
        usersRes <- dao.findUsersInGroup(groupId)
        res <- getUserDetailsFor(groupId, groupRes, usersRes)
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
   * @param email
   * @param gid
   * @param cid
   * @return
   */
  def revokeCollectionAuth(
    gid: String,
    email: String,
    cid: String
  ) = Action.async { implicit request =>
    Email.fromString(email).map { feideEmail =>
      GroupId.validate(gid).toOption.map(GroupId.apply).map { groupId =>
        CollectionUUID.validate(cid).toOption.map(CollectionUUID.apply).map { colId =>
          dao.revokeCollectionFor(feideEmail, groupId, colId).map {
            case MusitSuccess(res) =>
              Redirect(
                controllers.web.routes.GroupController.groupEditUser(gid, email)
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
    }.getOrElse {
      handleNotFound(s"Not a valid email: $email")
    }
  }

  def grantCollectionAuth(
    gid: String,
    email: String,
    cid: String
  ) = Action.async { implicit request =>
    Email.fromString(email).map { feideEmail =>
      GroupId.validate(gid).toOption.map(GroupId.apply).map { groupId =>
        CollectionUUID.validate(cid).toOption.map(CollectionUUID.apply).map { colId =>
          dao.addUserToGroup(feideEmail, groupId, Option(Seq(colId))).map {
            case MusitSuccess(res) =>
              Redirect(
                controllers.web.routes.GroupController.groupEditUser(gid, email)
              )

            case err: MusitError =>
              InternalServerError(views.html.error(err.message))
          }
        }.getOrElse {
          handleNotFound(s"Wrong uuid format: $cid")
        }
      }.getOrElse {
        handleNotFound(s"Wrong uuid format: $gid")
      }
    }.getOrElse {
      handleNotFound(s"Not a valid email: $email")
    }
  }

}
