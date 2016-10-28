package no.uio.musit.microservice.actor.resource

import com.google.inject.Inject
import no.uio.musit.microservice.actor.service.UserService
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

class UserResource @Inject() (
    val authService: Authenticator,
    val userService: UserService
) extends MusitController {

  def currentUser = MusitSecureAction().async { request =>
    val authUser = request.user
    userService.currenUserAsActor(authUser).map(p => Ok(Json.toJson(p)))
  }
}
