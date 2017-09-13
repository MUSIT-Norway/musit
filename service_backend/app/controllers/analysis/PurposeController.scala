package controllers.analysis

import com.google.inject.{Inject, Singleton}
import models.analysis.Purposes
import models.analysis.Purposes.Purpose
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents

@Singleton
class PurposeController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val authService: Authenticator
) extends MusitController {

  def listPurposes = MusitSecureAction(CollectionManagement) { implicit request =>
    Ok(Json.toJson(Purposes.purposes.map(Purpose.toJson)))

  }
}
