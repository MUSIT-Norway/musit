package controllers.web

import java.util.UUID

import com.google.inject.Inject
import models.{GroupAdd, GroupId}
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import services.GroupService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import play.api.i18n.{I18nSupport, MessagesApi}

class GroupController @Inject() (
    implicit
    groupService: GroupService,
    val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  case class GroupData(name: String, permission: Int, description: Option[String])

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  val groupForm = Form(
    mapping(
      "name" -> text(minLength = 3),
      "permission" -> number(min = 1),
      "description" -> optional(text)
    )(GroupData.apply)(GroupData.unapply)
  )

  def index() = Action { implicit request =>
    Redirect(controllers.web.routes.GroupController.groupAddGet())
  }

  def groupAddGet() = Action { implicit request =>
    Ok(views.html.groupAdd(groupForm, allowedGroups))
  }

  def groupAddPost() = Action.async { implicit request =>
    groupForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.groupAdd(formWithErrors, allowedGroups)))
      },
      groupData => {
        val groupAdd = new GroupAdd(
          groupData.name,
          Permission.fromInt(groupData.permission),
          groupData.description
        )
        groupService.add(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(controllers.web.routes.GroupController.groupList())
              .flashing("success" -> "Group added!")
          case error: MusitError =>
            InternalServerError(error.message)
        }
      }
    )
  }

  def groupList() = Action.async { implicit request =>
    groupService.allGroups.map {
      case MusitSuccess(groups) =>
        Ok(views.html.groupList(groups, None))
      case error: MusitError =>
        Ok(views.html.groupList(Seq.empty, Some(error)))
    }
  }

  def groupActors(groupUUID: String) = Action.async { implicit request =>
    val groupId = new GroupId(UUID.fromString(groupUUID))
    groupService.group(groupId).flatMap {
      case MusitSuccess(maybeGroup) => maybeGroup match {
        case Some(group) =>
          groupService.listUsersInGroup(groupId).map {
            case MusitSuccess(result) =>
              Ok(views.html.groupActors(result, group))
            case error: MusitError =>
              Ok(views.html.groupActors(Seq.empty, group, Some(error)))
          }
        case None =>
          Future.successful(
            NotFound(views.html.notFound(s"The group id $groupUUID was not found"))
          )
      }

      case error: MusitError =>
        Future.successful(
          NotFound(views.html.notFound(s"Failed to get group with id $groupUUID"))
        )
    }
  }

}
