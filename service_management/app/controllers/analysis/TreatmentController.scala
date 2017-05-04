package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.MuseumId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.analysis.TreatmentService

@Singleton
class TreatmentController @Inject()(
    val authService: Authenticator,
    val treatService: TreatmentService
) extends MusitController {

  def getTreatmentList =
    MusitSecureAction().async { implicit request =>
      treatService.getTreatmentList.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }
}
