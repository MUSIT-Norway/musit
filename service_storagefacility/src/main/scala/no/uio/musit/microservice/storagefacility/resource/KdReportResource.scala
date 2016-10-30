package no.uio.musit.microservice.storagefacility.resource

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.service.KdReportService
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

class KdReportResource @Inject() (
    val authService: Authenticator,
    val kdReportService: KdReportService
) extends MusitController {

  def getReportByMuseum(mid: Int) = MusitSecureAction(mid).async { implicit request =>
    kdReportService.getReport(mid).map {
      case MusitSuccess(reports) => Ok(Json.toJson(reports))
      case err: MusitError => InternalServerError(Json.obj("message" -> err.message))
    }
  }
}
