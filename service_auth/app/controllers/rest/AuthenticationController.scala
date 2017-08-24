package controllers.rest

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Logger}

/**
 * This controller will expose login and logout functionality for the MUSIT
 * system. It will handle the interaction with Dataporten to perform the OAuth2
 * authentication flow.
 */
class AuthenticationController @Inject()(
    implicit
    val controllerComponents: ControllerComponents,
    val conf: Configuration,
    val authService: Authenticator
) extends MusitController {

  private val logger = Logger(classOf[AuthenticationController])

  private val delphiCallback = conf.getOptional[String]("musit.delphi.callback")

  /**
   * Handles OAuth2 authentication flow against the configured Authenticator service.
   */
  def authenticate(client: Option[String] = None) = Action.async { implicit request =>
    authService.authenticate(client).map {
      case Left(res) => res
      case Right(userSession) =>
        logger.debug(s"Initialized new UserSesssion with id ${userSession.uuid}")
        val callbackUrl = userSession.client.flatMap {
          case Authenticator.ClientDelphi => delphiCallback
          case _                          => None
        }.getOrElse("/")

        Redirect(
          url = callbackUrl,
          queryString = Map("_at" -> Seq(s"${userSession.uuid.asString}"))
        )
    }
  }

  /**
   * Marks the UserSession associated with the bearer token in the request as
   * logged out.
   */
  def logout = MusitSecureAction().async { implicit request =>
    authService.invalidate(request.token).map {
      case MusitSuccess(()) => Ok
      case err: MusitError  => InternalServerError(Json.obj("message" -> err.message))
    }
  }

}
