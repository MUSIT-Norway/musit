package controllers

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import services.ObjectAggregationService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ObjectAggregationController @Inject() (
    service: ObjectAggregationService
) extends Controller {

  def getObjects(museumId: Long) = Action.async { request =>
    service.getObjects(museumId).map(__ => Ok(Json.toJson(__)))
  }

}
