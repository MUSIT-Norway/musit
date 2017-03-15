package controllers

import no.uio.musit.service.BuildInfo
import play.api.mvc._

class Application extends Controller {

  def buildInfo = Action { implicit request =>
    Ok(BuildInfo.toJson)
  }

}
