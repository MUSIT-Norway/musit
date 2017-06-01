package controllers.rest

import com.google.inject.{Inject, Singleton}
import no.uio.musit.models.Museums
import no.uio.musit.models.Museums.Museum
import no.uio.musit.security.Authenticator
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.service.MusitAdminController
import play.api.libs.json.Json

@Singleton
class MuseumController @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto
) extends MusitAdminController {

  def listMuseums = MusitSecureAction() { implicit request =>
    Ok(Json.toJson(Museums.museums.map(Museum.toJson)))
  }

}
