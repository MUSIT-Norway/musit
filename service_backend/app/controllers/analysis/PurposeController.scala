package controllers.analysis

import com.google.inject.{Inject, Singleton}
import models.analysis.Purposes
import models.analysis.Purposes.Purpose
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.json.Json

@Singleton
class PurposeController @Inject()(
    implicit val authService: Authenticator
) extends MusitController {

  def listPurposes = MusitSecureAction() { implicit request =>
    Ok(Json.toJson(Purposes.purposes.map(Purpose.toJson)))

  }
}
