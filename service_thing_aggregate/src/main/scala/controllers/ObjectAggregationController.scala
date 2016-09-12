package controllers

import com.google.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.ObjectAggregationService

class ObjectAggregationController @Inject() (
    service: ObjectAggregationService
) extends Controller {

  def getObjects(nodeId: Long) = Action.async { request =>
    service.getObjects(nodeId).map(__ => Ok(Json.toJson(__)))
  }

}
