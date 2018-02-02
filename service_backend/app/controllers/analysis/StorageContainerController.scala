package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.mvc.ControllerComponents
import services.analysis.StorageContainerService

@Singleton
class StorageContainerController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val scService: StorageContainerService
) extends MusitController {

  def getStorageContainerList =
    MusitSecureAction().async { implicit request =>
      scService.getStorageContainerList.map {
        case MusitSuccess(t) => listAsPlayResult(t)
        case err: MusitError => internalErr(err)
      }
    }
}
