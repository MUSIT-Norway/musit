package controllers.web

import com.google.inject.Inject
import models.GroupAdd._
import no.uio.musit.models.GroupId
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.GroupService

import scala.concurrent.Future

class GroupController @Inject() (
    implicit
    groupService: GroupService,
    val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  def groupAddActorGet = Action { implicit request =>
    Ok
  }

  def groupAddActorPost = Action.async { implicit request =>
    Future.successful(Ok)
  }

  def groupAddGet() = Action { implicit request =>
    Ok(views.html.groupAdd(groupAddForm, allowedGroups))
  }

  def groupAddPost() = Action.async { implicit request =>
    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(views.html.groupAdd(formWithErrors, allowedGroups))
        )
      },
      groupAdd => {
        groupService.add(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(
              controllers.web.routes.GroupController.groupList()
            ).flashing("success" -> "Group added!")
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

  def groupActorsList(groupUUID: String) = Action.async { implicit request =>
    GroupId.validate(groupUUID).toOption.map { uuid =>
      val groupId = GroupId.fromUUID(uuid)
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
              NotFound(views.html.notFound(s"The group $uuid was not found"))
            )
        }

        case error: MusitError =>
          Future.successful(
            NotFound(views.html.notFound(s"Failed to get group with id: $uuid"))
          )
      }
    }.getOrElse(
      Future.successful(
        NotFound(views.html.notFound(s"Wrong uuid format: $groupUUID"))
      )
    )
  }

}
