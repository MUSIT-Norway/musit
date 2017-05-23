package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers.{internalErr, listAsPlayResult}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.analysis.StorageMediumService

@Singleton
class StorageMediumController @Inject()(
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
