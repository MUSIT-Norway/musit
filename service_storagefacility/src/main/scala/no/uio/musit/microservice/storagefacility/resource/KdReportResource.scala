package no.uio.musit.microservice.storagefacility.resource

import com.google.inject.Inject
import play.api.mvc.{Action, Controller}
import no.uio.musit.microservice.storagefacility.service.KdReportService
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class KdReportResource @Inject() (val kdReportService: KdReportService) extends Controller {

  def getReportByMuseum = Action.async { implicit request =>
    kdReportService.getReport.map {
      case MusitSuccess(reports) =>
        Ok(Json.toJson(reports))
      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

}
