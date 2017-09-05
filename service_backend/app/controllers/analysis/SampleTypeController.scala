package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.mvc.ControllerComponents
import services.analysis.SampleTypeService

@Singleton
class SampleTypeController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val sampleTypeService: SampleTypeService
) extends MusitController {

  def getSampleTypeList =
    MusitSecureAction(CollectionManagement).async { implicit request =>
      sampleTypeService.getSampleTypeList.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }
}
