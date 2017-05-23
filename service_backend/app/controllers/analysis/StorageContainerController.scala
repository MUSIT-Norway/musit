package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import services.analysis.StorageContainerService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class StorageContainerController @Inject()(
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
