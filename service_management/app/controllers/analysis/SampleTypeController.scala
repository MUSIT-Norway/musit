package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import services.analysis.SampleTypeService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class SampleTypeController @Inject()(
    val authService: Authenticator,
    val sampleTypeService: SampleTypeService
) extends MusitController {

  def getSampleTypeList =
    MusitSecureAction().async { implicit request =>
      sampleTypeService.getSampleTypeList.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }
}
