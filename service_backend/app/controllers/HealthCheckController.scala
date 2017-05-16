package controllers

import com.google.inject.Inject
import no.uio.musit.healthcheck.HealthCheckService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class HealthCheckController @Inject()(healthCheckService: HealthCheckService)
    extends Controller {

  def healthCheck = Action.async { implicit request =>
    healthCheckService.executeHealthChecks().map(hc => Ok(Json.toJson(hc)))
  }

}
