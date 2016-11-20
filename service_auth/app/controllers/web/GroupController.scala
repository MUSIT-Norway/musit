package controllers.web

import com.google.inject.Inject
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

case class Actor(
  id: Int,
  fn: String,
  dataportenId: Option[String],
  applicationId: Option[String]
)

object Actor {
  implicit def format: Format[Actor] = Json.format[Actor]
}

class GroupController @Inject() (
    implicit
    groupService: GroupService,
    val messagesApi: MessagesApi,
    ws: WSClient,
    configuration: Configuration
) extends Controller with I18nSupport {

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  def deleteGroup(museumId: Int, groupUuid: String) = Action.async { implicit request =>
    val maybeGroupId = for {
      g <- GroupId.validate(groupUuid)
    } yield GroupId(g)
    maybeGroupId.toOption.map { groupId =>
      groupService.removeGroup(groupId).map {
        case MusitSuccess(int) =>
          Redirect(controllers.web.routes.GroupController.groupList(museumId))
            .flashing("success" -> "Group was removed")
        case error: MusitError =>
          BadRequest(
            Json.obj("error" -> error.message)
          )
      }
    }.getOrElse(
      Future.successful(
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for $groupUuid")
        )
      )
    )
  }

  def deleteUser(mId: Int, uId: String, gId: String) = Action.async { implicit request =>
    val ug = for {
      u <- ActorId.validate(uId)
      g <- GroupId.validate(gId)
    } yield (ActorId(u), GroupId(g))
    ug.toOption.map {
      case (uid, gid) =>
        groupService.removeUserFromGroup(uid, gid).map {
          case MusitSuccess(int) =>
            Redirect(controllers.web.routes.GroupController.groupActorsList(mId, gId))
              .flashing("success" -> "User was removed")
          case error: MusitError =>
            BadRequest(
              Json.obj("error" -> error.message)
            )
        }
    }.getOrElse {
      Future.successful {
        BadRequest(
          Json.obj("message" -> s"Invalid UUID for either $gId or $uId")
        )
      }
    }

  }

  def groupAddUserGet(museumId: Int, gId: String) = Action { implicit request =>
    Ok(views.html.groupUserAdd(userGroupAddForm, museumId, gId))
  }

  def groupAddUserPost(museumId: Int, gId: String) = Action.async { implicit request =>
    userGroupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupUserAdd(formWithErrors, museumId, gId))
        )
      },
      userAdd => {
        val userId = ActorId.validate(userAdd.userId).get
        val groupId = GroupId.validate(userAdd.groupId).get
        groupService.addUserToGroup(userId, groupId).map {
          case MusitSuccess(group) =>
            Redirect(
              controllers.web.routes.GroupController.groupActorsList(museumId, gId)
            ).flashing("success" -> "User added!")
          case error: MusitError =>
            BadRequest(
              Json.obj("error" -> error.message)
            )
        }
      }
    )
  }

  def groupAddGet(museumId: Int) = Action { implicit request =>
    Ok(views.html.groupAdd(groupAddForm, museumId, allowedGroups))
  }

  def groupAddPost(museumId: Int) = Action.async { implicit request =>
    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupAdd(formWithErrors, museumId, allowedGroups))
        )
      },
      groupAdd => {
        groupService.add(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(
              controllers.web.routes.GroupController.groupList(museumId)
            ).flashing("success" -> "Group added!")
          case error: MusitError =>
            BadRequest(
              Json.obj("error" -> error.message)
            )
        }
      }
    )
  }

  def groupList(museumId: Int) = Action.async { implicit request =>
    groupService.allGroups.map {
      case MusitSuccess(groups) =>
        Ok(views.html.groupList(groups, museumId, None))
      case error: MusitError =>
        Ok(views.html.groupList(Seq.empty, museumId, Some(error)))
    }
  }

  def getActorsEndpoint(id: String) = Action.async { implicit request =>
    getActors(Seq(id)).map { actors =>
      Ok(Json.toJson(actors))
    }
  }

  def getActors(id: Seq[String]): Future[Seq[Actor]] = {
    configuration.getString("actor.detailsUrl").map { url =>
      ws.url(url)
        .withHeaders(
          "Authorization" -> "Bearer fake-token-zab-xy-normal"
        )
        .post(Json.toJson(id))
        .map(_.json.validate[Seq[Actor]] match {
          case JsSuccess(actors, _) => actors
          case JsError(error) => throw new IllegalStateException(error.mkString(", "))
        })
    }.getOrElse(throw new IllegalStateException("Missing actor url"))
  }

  def groupActorsList(museumId: Int, gUuid: String) = Action.async { implicit request =>
    val maybeGroupId = for {
      g <- GroupId.validate(gUuid)
    } yield GroupId(g)
    maybeGroupId.toOption.map { groupId =>
      groupService.group(groupId).flatMap {
        case MusitSuccess(maybeGroup) => maybeGroup match {
          case Some(group) =>
            groupService.listUsersInGroup(groupId).map {
              case MusitSuccess(result) =>
                Ok(views.html.groupActors(result, museumId, group))
              case error: MusitError =>
                Ok(views.html.groupActors(Seq.empty, museumId, group, Some(error)))
            }
          case None =>
            Future.successful(
              NotFound(
                views.html.notFound(s"The group ${groupId.asString} was not found")
              )
            )
        }
        case error: MusitError =>
          Future.successful(
            NotFound(
              views.html.notFound(s"Failed to get group with id: ${groupId.asString}")
            )
          )
      }
    }.getOrElse(
      Future.successful(
        NotFound(views.html.notFound(s"Wrong uuid format: $gUuid"))
      )
    )
  }

}
