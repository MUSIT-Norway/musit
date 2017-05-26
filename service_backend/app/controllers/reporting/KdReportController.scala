package controllers.reporting

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import services.reporting.KdReportService

class KdReportController @Inject()(
    val authService: Authenticator,
    val kdReportService: KdReportService
) extends MusitController {

  def getReport(mid: Int) = MusitSecureAction(mid, Read).async { implicit request =>
    kdReportService.getReport(mid).map {
      case MusitSuccess(reports) =>
        Ok(Json.toJson(reports))

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }
}
