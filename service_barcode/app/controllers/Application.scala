package controllers

import no.uio.musit.service.BuildInfo
import play.api.mvc.{Action, Controller}

class Application extends Controller {

  def buildInfo = Action { implicit request =>
    Ok(BuildInfo.toJson)
  }

}
