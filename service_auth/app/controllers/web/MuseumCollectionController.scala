package controllers.web

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.{Authenticator, EncryptedToken}
import no.uio.musit.service.MusitAdminController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.auth.dao.AuthDao

class MuseumCollectionController @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  def listCollections = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    dao.allCollections.map {
      case MusitSuccess(cols) => Ok(views.html.collections(request.user, encTok, cols))
      case err: MusitError =>
        InternalServerError(views.html.error(request.user, encTok, err.message))
    }
  }

}
