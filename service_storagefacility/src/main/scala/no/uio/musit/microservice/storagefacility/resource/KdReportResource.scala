package no.uio.musit.microservice.storagefacility.resource

import com.google.inject.Inject
import play.api.mvc.{Action, Controller}
import no.uio.musit.microservice.storagefacility.service.KdReportService
import no.uio.musit.models.Museums.Museum
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class KdReportResource @Inject() (val kdReportService: KdReportService) extends Controller {

  def getReportByMuseum(mid: Int) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museumId =>
      kdReportService.getReport(mid).map {
        case MusitSuccess(reports) =>
          Ok(Json.toJson(reports))
        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }
}
