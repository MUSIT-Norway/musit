package controllers.web

import com.google.inject.Inject
import no.uio.musit.security.{Authenticator, EncryptedToken}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.Logger

class Dashboard @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto
) extends MusitAdminController {

  val logger = Logger(classOf[Dashboard])

  def index = MusitAdminAction() { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    Ok(views.html.index(request.user, encTok))
  }
}
