package controllers

import play.api.mvc._
import no.uio.musit.service.BuildInfo

class Application extends Controller {

  def buildInfo = Action { implicit request =>
    Ok(BuildInfo.toJson)
  }

}
