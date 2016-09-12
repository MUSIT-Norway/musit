package controllers

import com.google.inject.Inject
import play.api.mvc._
import services.ObjectAggregationService

import scala.concurrent.Future

class ObjectAggregationController @Inject() (
    service: ObjectAggregationService
) extends Controller {

  def getObjects(museumId: Long) = Action.async { request =>
    Future.successful(Ok("Hei"))
  }

}
