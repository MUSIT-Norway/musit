package controllers.web

import com.google.inject.Inject
import no.uio.musit.models.Museums
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.{Authenticator, EncryptedToken}
import no.uio.musit.service.MusitAdminController
import play.api.mvc.ControllerComponents
import repositories.auth.dao.AuthDao

class MuseumController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  def listMuseums = MusitAdminAction() { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    val museums = {
      if (request.user.hasGodMode) Museums.museums.filterNot(_ == Museums.All)
      else Museums.museums.filter(m => request.user.isAuthorized(m.id))
    }
    Ok(views.html.museumList(request.user, encTok, museums))
  }

}
