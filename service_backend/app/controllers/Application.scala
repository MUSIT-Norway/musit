package controllers

import com.google.inject.Inject
import play.api.mvc._
import no.uio.musit.service.BuildInfo

class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def buildInfo = Action(implicit request => Ok(BuildInfo.toJson))

}
