package controllers.actor

import com.google.inject.Inject
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import services.actor.UserService

class UserController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val userService: UserService
) extends MusitController {

  def currentUser = MusitSecureAction().async { request =>
    val authUser = request.user
    userService.currentUserAsActor(authUser).map(p => Ok(Json.toJson(p)))
  }
}
