package controllers.web

import com.google.inject.Inject
import models.{Actor, Group}
import models.GroupAdd._
import models.UserGroupAdd._
import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitResults.{MusitError, MusitResult, MusitSuccess}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.GroupService

import scala.concurrent.Future
import scala.util.control.NonFatal

class GroupController @Inject() (
    implicit
    val groupService: GroupService,
    val messagesApi: MessagesApi,
    val ws: WSClient,
    val configuration: Configuration
) extends Controller with I18nSupport {

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  def deleteGroup(mid: Int, gid: String) = Action.async { implicit request =>
    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      groupService.removeGroup(groupId).map {
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

  def deleteUser(
    mid: Int,
    email: String,
    gid: String
  ) = Action.async { implicit request =>
    GroupId.validate(gid).toOption.map(GroupId.apply).map { gid =>
      groupService.removeUserFromGroup(email, gid).map {
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

  def groupAddUserGet(mid: Int, gid: String) = Action { implicit request =>
    Ok(views.html.groupUserAdd(userGroupAddForm, mid, gid))
  }

  def groupAddUserPost(mid: Int, gid: String) = Action.async { implicit request =>
    userGroupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupUserAdd(formWithErrors, mid, gid))
        )
      },
      userAdd => {
        val userId = userAdd.userId.flatMap { uid =>
          ActorId.validate(uid).toOption
        }.map(ActorId.apply)
        val groupId = GroupId.validate(userAdd.groupId).get
        groupService.addUserToGroup(userAdd.email, groupId, userId).map {
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
  }

  def groupAddGet(mid: Int) = Action { implicit request =>
    Ok(views.html.groupAdd(groupAddForm, mid, allowedGroups))
  }

  def groupAddPost(mid: Int) = Action.async { implicit request =>
    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupAdd(formWithErrors, mid, allowedGroups))
        )
      },
      groupAdd => {
        groupService.add(groupAdd).map {
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

  def groupList(mid: Int) = Action.async { implicit request =>
    groupService.allGroups.map {
      case MusitSuccess(groups) =>
        Ok(views.html.groupList(groups, mid, None))
      case error: MusitError =>
        Ok(views.html.groupList(Seq.empty, mid, Some(error)))
    }
  }

  def getActors(ids: Seq[String]): Future[Either[String, Seq[Actor]]] = {
    configuration.getString("actor.detailsUrl").map { url =>
      ws.url(url)
        .withHeaders(
          "Authorization" -> "Bearer fake-token-zab-xy-superuser"
        )
        .post(Json.toJson(ids))
        .map { res =>
          res.status match {
            case 200 => res.json.validate[Seq[Actor]] match {
              case JsSuccess(actors, _) => Right(actors)
              case JsError(error) => Left(error.mkString(", "))
            }
            case _ => Left("No content (ish)")
          }
        }
    }.getOrElse(Future.successful(Left("Missing actor url")))
  }

  private def handleNotFound(msg: String): Future[Result] = {
    Future.successful(NotFound(views.html.error(msg)))
  }

  def getActorDetailsFor(
    mid: Int,
    groupRes: MusitResult[Option[Group]],
    usersRes: MusitResult[Seq[String]]
  ): Result = {
    groupRes.flatMap { group =>
      usersRes.map { users =>
        // TODO: We should call getActors(users) if we have an ActorId
        group.map { grp =>
          Ok(views.html.groupActors(users, mid, grp))
        }.getOrElse {
          NotFound(views.html.error(s"Could not find group"))
        }
      }
    }.getOrElse {
      InternalServerError(views.html.error("An error occurred fetching the group"))
    }
  }

  def groupActorsList(mid: Int, gid: String) = Action.async { implicit request =>
    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      (for {
        groupRes <- groupService.group(groupId)
        usersRes <- groupService.listUsersInGroup(groupId)
      } yield {
        getActorDetailsFor(mid, groupRes, usersRes)
      }).recover {
        case NonFatal(ex) =>
          InternalServerError(views.html.error("An error occurred fetching data"))
      }
    }.getOrElse {
      handleNotFound(s"Wrong uuid format: $gid")
    }
  }

}
