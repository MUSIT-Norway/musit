package controllers

import com.google.inject.Inject
import no.uio.musit.healthcheck.HealthCheckService
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class HealthCheckController @Inject()(
    cc: ControllerComponents,
    healthCheckService: HealthCheckService
) extends AbstractController(cc) {

  implicit val ec: ExecutionContext = cc.executionContext

  def healthCheck = Action.async { implicit request =>
    healthCheckService.executeHealthChecks().map(hc => Ok(Json.toJson(hc)))
  }

}
