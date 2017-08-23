package controllers.rest

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.Email
import no.uio.musit.security.Authenticator
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Result}
import repositories.auth.dao.AuthDao

import scala.concurrent.Future

@Singleton
class GroupController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  val logger = Logger(classOf[GroupController])

  private def serverError(msg: String): Result =
    InternalServerError(Json.obj("message" -> msg))

  def groupsForUser(
      email: String
  ) = MusitSecureAction().async { implicit request =>
    Email
      .validate(email)
      .map { feideEmail =>
        dao.findGroupInfoFor(feideEmail).map {
          case MusitSuccess(grps) =>
            if (grps.nonEmpty) Ok(Json.toJson(grps)) else NoContent
          case err: MusitError => serverError(err.message)
        }
      }
      .getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"Invalid email $email"))
        }
      }
  }

}
