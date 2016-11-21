package controllers.web

import com.google.inject.Inject
import models.Actor
import models.GroupAdd._
import models.UserGroupAdd._
import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.GroupService

import scala.concurrent.Future

class GroupController @Inject()(
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

  def deleteUser(mid: Int, uid: String, gid: String) = Action.async { implicit request =>
    val ug = for {
      u <- ActorId.validate(uid)
      g <- GroupId.validate(gid)
    } yield (ActorId(u), GroupId(g))
    ug.toOption.map {
      case (uid, gid) =>
        groupService.removeUserFromGroup(uid, gid).map {
          case MusitSuccess(int) =>
            Redirect(controllers.web.routes.GroupController.groupActorsList(mid, gid))
              .flashing("success" -> "User was removed")
          case error: MusitError =>
            BadRequest(
              Json.obj("error" -> error.message)
            )
        }
    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for either $gid or $uid")
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
        val userId = ActorId.validate(userAdd.userId).get
        val groupId = GroupId.validate(userAdd.groupId).get
        groupService.addUserToGroup(userId, groupId).map {
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
    Future.successful(NotFound(views.html.notFound(msg)))
  }

  def groupActorsList(mid: Int, gid: String) = Action.async { implicit request =>
    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      groupService.group(groupId).flatMap {
        case MusitSuccess(maybeGroup) =>
          maybeGroup.map { group =>
            groupService.listUsersInGroup(groupId).flatMap {
              case MusitSuccess(result) =>
                getActors(result.map(_.asString)).map {
                  case Right(actors) =>
                    Ok(views.html.groupActors(result, mid, group, actors))

                  case Left(error) =>
                    // TODO log error here
                    Ok(views.html.groupActors(result, mid, group, Seq.empty))
                }
              case error: MusitError =>
                Future.successful(
                  Ok(views.html.groupActors(
                    Seq.empty, mid, group, Seq.empty, Some(error)
                  ))
                )
            }
          }.getOrElse {
            handleNotFound(s"The group ${groupId.asString} was not found")
          }
        case error: MusitError =>
          handleNotFound(s"Failed to get group with id: ${groupId.asString}")
      }
    }.getOrElse {
      handleNotFound(s"Wrong uuid format: $gid")
    }
  }

}
