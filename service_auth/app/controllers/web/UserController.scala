package controllers.web

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.{Authenticator, EncryptedToken}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.actor.dao.AuthDao

class UserController @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  def users = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    dao.allUsers.map {
      case MusitSuccess(usrs) => Ok(views.html.users(encTok, usrs))
      case err: MusitError    => InternalServerError(views.html.error(encTok, err.message))
    }
  }

}
