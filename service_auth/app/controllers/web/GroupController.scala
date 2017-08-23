package controllers.web

import com.google.inject.Inject
import controllers.web
import models.GroupAdd._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security.Permissions._
import no.uio.musit.security._
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.ControllerComponents
import repositories.auth.dao.AuthDao

import scala.concurrent.Future

class GroupController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao,
    val configuration: Configuration
) extends MusitAdminController
    with I18nSupport {

  val allowedGroups = scala.collection.immutable.Seq(
    (GodMode.priority.toString, GodMode.productPrefix),
    (Admin.priority.toString, Admin.productPrefix),
    (Write.priority.toString, Write.productPrefix),
    (Read.priority.toString, Read.productPrefix),
    (Guest.priority.toString, Guest.productPrefix)
  )

  val allowedModules = scala.collection.immutable.Seq(
    (StorageFacility.id.toString, StorageFacility.productPrefix),
    (CollectionManagement.id.toString, CollectionManagement.productPrefix)
  )

  /**
   *
   * @param gid
   * @return
   */
  def deleteGroup(gid: String) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    val maybeGroupId = GroupId.validate(gid).toOption.map(GroupId.apply)
    maybeGroupId.map { groupId =>
      dao.deleteGroup(groupId).map {
        case MusitSuccess(int) =>
          Redirect(
            url = web.routes.GroupController.groupList().url,
            queryString = Map("_at" -> Seq(encTok.asString))
          ).flashing("success" -> "Group was removed")
        case error: MusitError =>
          badRequest(request.user, encTok, error.message)
      }
    }.getOrElse(badRequestF(request.user, encTok, s"Invalid UUID for $gid"))
  }

  def groupAddGet = MusitAdminAction() { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    Ok(
      views.html
        .groupAdd(request.user, encTok, groupAddForm, allowedGroups, allowedModules)
    )
  }

  def groupAddPost = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    groupAddForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(
          BadRequest(
            views.html.groupAdd(
              request.user,
              encTok,
              formWithErrors,
              allowedGroups,
              allowedModules
            )
          )
        )
      },
      groupAdd => {
        dao.addGroup(groupAdd).map {
          case MusitSuccess(group) =>
            Redirect(
              url = web.routes.GroupController.groupList().url,
              queryString = Map("_at" -> Seq(encTok.asString))
            ).flashing("success" -> "Group added!")
          case error: MusitError =>
            badRequest(request.user, encTok, error.message)
        }
      }
    )
  }

  def groupList = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    dao.allGroups.map {
      case MusitSuccess(groups) =>
        val grps = groups.filterNot(_.permission == Permissions.GodMode)
        Ok(views.html.groupList(request.user, encTok, grps, None))
      case error: MusitError =>
        Ok(views.html.groupList(request.user, encTok, Seq.empty, Some(error)))
    }
  }

}
