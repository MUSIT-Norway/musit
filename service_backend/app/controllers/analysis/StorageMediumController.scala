package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.mvc.ControllerComponents
import services.analysis.StorageMediumService

@Singleton
class StorageMediumController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val smService: StorageMediumService
) extends MusitController {

  def getStorageMediumList =
    MusitSecureAction().async { implicit request =>
      smService.getStorageMediumList.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }
}
