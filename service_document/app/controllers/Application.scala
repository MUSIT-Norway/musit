package controllers

import com.google.inject.Inject
import no.uio.musit.service.BuildInfo
import play.api.mvc.{AbstractController, ControllerComponents}

class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def buildInfo = Action(implicit request => Ok(BuildInfo.toJson))

}
