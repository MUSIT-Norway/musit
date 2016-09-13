package no.uio.musit.microservice.actor.resource

import com.google.inject.Inject
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservice.actor.service.{ PersonService, UserService }
import no.uio.musit.microservices.common.utils.ResourceHelper
import no.uio.musit.security.Security
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ResourceHelper
import no.uio.musit.microservices.common.utils.ResourceHelper._

/**
 * Created by jarle on 13.09.16.
 */
class UserResource @Inject() (userService: UserService) extends Controller {

  def getCurrentUserAsActor: Action[AnyContent] = Action.async { request =>
    val futPerson = Security.create(request).musitFutureFlatMap { securityConnection =>
      userService.getCurrentUserAsActor(securityConnection)
    }
    ResourceHelper.getRoot(futPerson, (p: Person) => Json.toJson(p))
  }
}
