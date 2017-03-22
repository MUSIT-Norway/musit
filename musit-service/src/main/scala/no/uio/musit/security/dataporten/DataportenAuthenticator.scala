package no.uio.musit.security.dataporten

import com.google.inject.Inject
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.Email
import no.uio.musit.security._
import no.uio.musit.security.dataporten.DataportenAuthenticator._
import no.uio.musit.security.oauth2.{OAuth2Constants, OAuth2Info}
import no.uio.musit.time.dateTimeNow
import no.uio.musit.ws.ViaProxy.viaProxy
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.duration._

case class DataportenAuthenticatorConfig(
    sessionTimeout: FiniteDuration,
    authorizationURL: String,
    accessTokenURL: String,
    userApiURL: String,
    callbackURL: String,
    clientId: Option[ClientId],
    clientSecret: String
)

/**
 * Service for communicating with Dataporten
 *
 * @param conf         The Play! Configuration instance
 * @param authResolver Instance for resolving a users groups
 * @param ws           Play! WebService client
 */
class DataportenAuthenticator @Inject()(
    authResolver: AuthResolver,
    ws: WSClient
)(implicit conf: Configuration)
    extends Authenticator
    with OAuth2Constants {

  private val logger = Logger(classOf[DataportenAuthenticator])

  private type DataportenToken = BearerToken
  private type AuthResponse    = Future[Either[Result, UserSession]]

  implicit val clientIdReader = new ValueReader[Option[ClientId]] {
    def read(config: Config, path: String): Option[ClientId] = {
      ClientId.validate(config.getString(path)).toOption.map(ClientId.apply)
    }
  }
  val config = conf.underlying.as[DataportenAuthenticatorConfig]("musit.dataporten")

  /**
   * Starts the OAuth2 authentication process. Here's an explanation of how this
   * process works:
   *
   * 1. The first request in the OAuth2 process is a general "authorization"
   * request to the OAuth2 provider. This will trigger a new UserSession to be
   * initialised in the database that is backing the AuthResolver. Then it will
   * send redirect response to the provider login form.
   *
   * 2. When the provider login dialog is completed, the provider will send a
   * "code" to the configured callback URL. In our case the authentication
   * endpoint.
   *
   * 3. We then attempt to extract the given "code", and use it to request a new
   * "access token" from the provider. Which is received in an OAuth2Info
   * response.
   *
   * 4. Once we have the "access token" we try to fetch the user info from the
   * provider.
   *
   * 5. With both the OAuth2Info and UserInfo, we can now update the user session
   * with the information we've received.
   *
   * 6. We can finally return our generated SessionUUID as the token clients
   * should use as the Bearer token in the HTTP Authorization header.
   *
   * If any single one of the steps above should fail, the process will result
   * in an "Unauthorized" response.
   *
   * @param client The client app making the authenticate request
   * @param req    The current request.
   * @tparam A The type of the request body.
   * @return Either a Result or the active UserSession
   */
  override def authenticate[A](
      client: Option[String]
  )(implicit req: Request[A]): AuthResponse =
    handleAuthResponse(client) { code =>
      logger.debug(s"Got code $code. Trying to fetch access token from Dataporten...")
      getToken(code).flatMap {
        case Right(oauthInfo) =>
          // Extract the OAuth2 state from the request
          extractParam(State)
            .flatMap(s => SessionUUID.validate(s).toOption)
            .map { sid =>
              val procRes = for {
                maybeSession <- MusitResultT(authResolver.userSession(sid))
                userInfo     <- MusitResultT(userInfoDataporten(oauthInfo.accessToken))
                _            <- MusitResultT(authResolver.saveUserInfo(userInfo))
              } yield {
                maybeSession.map(
                  _.activate(oauthInfo, userInfo, config.sessionTimeout.toMillis)
                )
              }

              procRes.value.flatMap {
                case MusitSuccess(maybeSession) =>
                  logger.debug(s"Found session in DB: $maybeSession")
                  maybeSession.map { session =>
                    // Update the user session with the Oauth2Info and UserInfo.
                    authResolver.updateSession(session).map {
                      case MusitSuccess(()) => Right(session)
                      case err: MusitError =>
                        logger.error(err.message)
                        Left(Results.Unauthorized)
                    }
                  }.getOrElse {
                    logger.error(
                      s"The OAuth2 state $sid did not match any " +
                        s"initialised sessions. This could indicate attempts to spoof" +
                        s"the OAuth2 process."
                    )
                    Future.successful(Left(Results.Unauthorized))
                  }

                case err: MusitError =>
                  logger.error(err.message)
                  Future.successful(Left(Results.Unauthorized))

              }
            }
            .getOrElse {
              logger.error(
                "Bad state value received from Dataporten. This could "
                  + "indicate attempts to spoof the OAuth2 process."
              )
              Future.successful(Left(Results.Unauthorized))
            }

        case Left(res) => Future.successful(Left(res))
      }
    }

  /**
   * Method to "touch" the UserSession whenever a User interacts with a service.
   *
   * @param token BearerToken
   * @return eventually it returns the updated MusitResult[UserSession]
   */
  def touch(token: BearerToken): Future[MusitResult[UserSession]] = {
    logger.debug("Registering UserSession activity.")
    val sid = SessionUUID.fromBearerToken(token)
    MusitResultT(authResolver.userSession(sid)).flatMap { maybeSession =>
      for {
        _ <- delphiAuth(token, maybeSession)
        us <- MusitResultT(updateSession(token) { session =>
               session.tokenExpiry.map {
                 expiration =>
                   val now = dateTimeNow
                   // Check if the session has expired
                   if (now.isBefore(expiration)) {
                     val u = session.touch(config.sessionTimeout.toMillis)
                     MusitResultT(authResolver.updateSession(u)).map(_ => u)
                   } else {
                     val msg = "Session has expired. Invalidating session."
                     logger.warn(msg)
                     invalidateWithError[UserSession](token)(MusitNotAuthenticated(msg))
                   }
               }.getOrElse {
                 val msg = "Session is not valid."
                 logger.error(msg)
                 invalidateWithError[UserSession](token)(MusitInternalError(msg))
               }
             })
      } yield us
    }.value
  }

  //============================================================================
  // TODO: Remove me when Delphi has updated its login handling
  private def delphiAuth(
      token: BearerToken,
      maybeSession: Option[UserSession]
  ): MusitResultT[Future, UserSession] = {
    val now = dateTimeNow
    maybeSession.map { s =>
      // So...we have a session. To identify if the request comes from a Delphi
      // client, we need to check the presence of "loginTime". If it isn't set,
      // we can assume the session belongs to a Delphi client. Then we need to
      // override any indication of an invalidated session by re-setting both
      // the "isLoggedIn", "lastActive" and "tokenExpiry" values.
      // If the session _has_ a "loginTime" value, it is a regular session and
      // we do nothing more.
      val expired = s.tokenExpiry.exists(now.isAfter)
      if (s.loginTime.isEmpty && (!s.isLoggedIn || expired)) {
        logger.warn(s"Resetting invalidated Delphi session.")
        val us = s.copy(
          lastActive = Option(now),
          isLoggedIn = true,
          tokenExpiry = Option(now.plus(config.sessionTimeout.toMillis).getMillis)
        )
        MusitResultT(authResolver.upsertUserSession(us)).map(_ => us)
      } else {
        MusitResultT(Future.successful[MusitResult[UserSession]](MusitSuccess(s)))
      }
    }.getOrElse {
      logger.warn(s"Delphi application authenticating using Dataporten token.")
      for {
        // We need to validate the token in Dataporten by calling the userInfo service.
        userInfo <- MusitResultT(userInfoDataporten(token))
        // Then we need to save it, in case the info changed.
        _ <- MusitResultT(authResolver.saveUserInfo(userInfo))
        // since we can safely assume we have no previously initialised
        // UserSession by arriving here, we initialise one now.
        session <- {
          val s = UserSession(
            // Using the oauth token from Dataporten as SessionUUID. This is not
            // optimal, but necessary to reduce impact of support for the legacy
            // login process.
            uuid = SessionUUID.fromBearerToken(token),
            oauthToken = Option(token),
            userId = Option(userInfo.id),
            lastActive = Option(now),
            isLoggedIn = true,
            tokenExpiry = Option(now.plus(config.sessionTimeout.toMillis).getMillis)
          )
          MusitResultT(authResolver.upsertUserSession(s)).map(_ => s)
        }
      } yield session
    }
  }

  //======================= Remove until here ==================================

  private def invalidateWithError[A](
      token: BearerToken
  )(err: MusitError): MusitResultT[Future, A] = {
    for {
      _   <- MusitResultT(invalidate(token))
      res <- MusitResultT(Future.successful[MusitResult[A]](err))
    } yield res
  }

  /**
   * Invalidates/Terminates the UserSession associated with the given token.
   *
   * @param token BearerToken
   * @return a MusitResult[Unit] wrapped in a Future.
   */
  override def invalidate(token: BearerToken): Future[MusitResult[Unit]] = {
    logger.debug("Invalidating UserSession.")
    updateSession(token) { session =>
      val u = session.copy(
        lastActive = Option(dateTimeNow),
        isLoggedIn = false
      )
      MusitResultT(authResolver.updateSession(u))
    }
  }

  /**
   * Retrieve the persisted UserInfo
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  override def userInfo(token: BearerToken): Future[MusitResult[UserInfo]] = {
    val sessionUUID = SessionUUID.fromBearerToken(token)
    (for {
      maybeSession <- MusitResultT(authResolver.userSession(sessionUUID))
      userInfo <- maybeSession.map { session =>
                   MusitResultT(userInfoFromSession(session))
                 }.getOrElse {
                   val msg = s"There is no session with ID $sessionUUID"
                   logger.warn(msg)
                   MusitResultT(
                     Future.successful[MusitResult[UserInfo]](MusitValidationError(msg))
                   )
                 }
    } yield userInfo).value
  }

  /**
   * Method for retrieving the users GroupInfo from the AuthService based
   * on the UserInfo found.
   *
   * @param userInfo the UserInfo found by calling the userInfo method above.
   * @return Will eventually return a Seq of GroupInfo
   */
  override def groups(userInfo: UserInfo): Future[MusitSuccess[Seq[GroupInfo]]] = {

    def stripPrefix(s: String): String = s.reverse.takeWhile(_ != ':').reverse.trim

    userInfo.secondaryIds.map { sids =>
      Future.sequence {
        sids.map(stripPrefix).filter(_.contains("@")).map { sid =>
          Email
            .validate(sid)
            .map { email =>
              authResolver.findGroupInfoByFeideEmail(email).map(_.getOrElse(Seq.empty))
            }
            .getOrElse(Future.successful(Seq.empty))
        }
      }.map(t => MusitSuccess(t.flatten))
    }.getOrElse {
      Future.successful(MusitSuccess(Seq.empty))
    }
  }

  private def updateSession[A](
      token: BearerToken
  )(update: UserSession => MusitResultT[Future, A]): Future[MusitResult[A]] = {
    val sid = SessionUUID.fromBearerToken(token)
    MusitResultT(authResolver.userSession(sid)).flatMap {
      case Some(session) =>
        if (session.isLoggedIn) update(session)
        else
          MusitResultT[Future, A](Future.successful {
            MusitNotAuthenticated("Session is no longer active.")
          })

      case None =>
        val msg = s"There is no session with ID $sid"
        logger.warn(msg)
        MusitResultT[Future, A](Future.successful(MusitValidationError(msg)))

    }.value
  }

  private def validateWSResponse[A, B](
      res: WSResponse
  )(f: WSResponse => MusitResult[A]): MusitResult[A] = {
    res.status match {
      case ok: Int if ok == Status.OK =>
        logger.debug(s"Request contained a valid bearer token")
        logger.debug(s"Validating audience...")
        // If the audience doesn't equal the clientId, the user isn't authorized
        val audience = (res.json \ "audience").as[ClientId]
        val usr      = (res.json \ "user" \ "userid").as[String]
        if (config.clientId.contains(audience)) {
          f(res)
        } else {
          logger.warn(s"Access attempt with wrong clientId $audience by user $usr")
          MusitNotAuthorized()
        }

      case ko: Int if ko == Status.UNAUTHORIZED =>
        logger.debug(s"Dataporten token was not valid.")
        MusitNotAuthenticated()

      case ko: Int =>
        logger.error(unexpectedResponseCode.format(ko))
        MusitInternalError(unexpectedResponseCode.format(ko))
    }
  }

  /**
   * Helper method to extract request parameters from the callback requests from
   * the Dataporten OAuth2 process.
   */
  private def extractParam[A](param: String)(implicit req: Request[A]): Option[String] = {
    req.queryString.get(param).flatMap(_.headOption)
  }

  /**
   * Helper method to fetch an access token from Dataporten.
   */
  private def getToken(
      oauthCode: String
  )(implicit req: RequestHeader): Future[Either[Result, OAuth2Info]] = {
    val params = Map(
      ClientID     -> Seq(config.clientId.map(_.asString).getOrElse("")),
      ClientSecret -> Seq(config.clientSecret),
      GrantType    -> Seq(AuthorizationCode),
      Code         -> Seq(oauthCode),
      RedirectURI  -> Seq(config.callbackURL)
    )

    ws.url(config.accessTokenURL)
      .viaProxy
      .post(params)
      .map(
        _.json.validate[OAuth2Info] match {
          case err: JsError =>
            val msg = "Invalid JSON response from Dataporten"
            logger.warn(s"$msg: ${Json.prettyPrint(JsError.toJson(err))}")
            Left(Results.InternalServerError(Json.obj("message" -> msg)))

          case JsSuccess(oi, _) =>
            logger.debug("Successfully retrieved an access token from Dataporten")
            Right(oi)
        }
      )
  }

  /**
   * Initialize a new persistent user session.
   */
  private def initSession(client: Option[String]): AuthResponse = {
    authResolver.sessionInit(client).map {
      case MusitSuccess(sessionId) =>
        // Set the request params for the Dataporten authorization service.
        val params = Map(
          ClientID     -> Seq(config.clientId.map(_.asString).getOrElse("")),
          RedirectURI  -> Seq(config.callbackURL),
          ResponseType -> Seq(Code),
          // Note that the OAuth2 "state" parameter is set to the sessionId
          // that was assigned when initializing the session. This allows
          // the state in subsequent callbacks to be validated against the
          // stored value.
          State -> Seq(sessionId.asString)
        )

        logger.trace(
          s"Using auth URL: ${config.authorizationURL} with params " +
            s"${params.map(p => s"${p._1}=${p._2.head}").mkString("?", "&", "")}"
        )

        // Redirecting to the configured auth URL to get the one-time code.
        Left(Results.Redirect(config.authorizationURL, params))

      case err: MusitError =>
        logger.error(s"An error occurred initialising the UserSession. ${err.message}")
        Left(Results.Unauthorized)
    }
  }

  /**
   * Helper function to process the authorization response from Dataporten.
   */
  private def handleAuthResponse[A](client: Option[String])(
      f: String => AuthResponse
  )(implicit req: Request[A]): AuthResponse = {
    // First check if request contains an Error
    extractParam(Error).map {
      case AccessDenied => Left(Results.Unauthorized)
      case e            => Left(Results.Unauthorized(Json.obj("message" -> e)))
    }.map(Future.successful).getOrElse {
      logger.trace(s"Request headers: ${req.headers.toMap.mkString("\n", "\n", "")}")
      logger.trace(s"Request params: ${req.queryString.mkString("\n", "\n", "")}")
      // Now check if the request contains a Code, call service to get token.
      // If there is no code, this is the first step in the OAuth2 flow.
      extractParam(Code) match {
        case Some(code) =>
          logger.debug("Attempting to get access token from Dataporten.")
          f(code)

        case None =>
          logger.debug("Initializing OAuth2 process with Dataporten.")
          initSession(client)
      }
    }
  }

  /**
   * Method for fetching UserInfo data from Dataporten service.
   *
   * @param token DataportenToken
   * @return eventually a MusitResult[UserInfo]
   */
  private def userInfoDataporten(
      token: DataportenToken
  ): Future[MusitResult[UserInfo]] = {
    ws.url(config.userApiURL).viaProxy.withHeaders(token.asHeader).get().map {
      response =>
        validateWSResponse(response) { res =>
          // The user info part of the message is always under the "user" key.
          // So we get it explicitly to deserialize to an UserInfo instance.
          val usrInfoJson = (response.json \ userInfoJsonKey).as[JsObject]
          usrInfoJson.validate[UserInfo] match {
            case JsSuccess(userInfo, _) =>
              MusitSuccess(userInfo)

            case err: JsError =>
              val prettyError = Json.prettyPrint(JsError.toJson(err))
              logger.error(unableToParse.format(prettyError))
              MusitInternalError(unableToParse.format(prettyError))
          }
        }
    }
  }

  /**
   * Find the UserInfo associated with the given UserSession
   *
   * @param session UserSession
   * @return eventually a MusitResult[UserInfo]
   */
  private def userInfoFromSession(session: UserSession): Future[MusitResult[UserInfo]] =
    session.userId.map { uid =>
      authResolver
        .userInfo(uid)
        .map(_.flatMap {
          case Some(ui) =>
            MusitSuccess(ui)

          case None =>
            val msg = s"Bad state. No user info for session ${session.uuid} exists."
            logger.error(msg)
            MusitInternalError(msg)
        })
    }.getOrElse {
      Future.successful(MusitValidationError("Session has no oauth2 token."))
    }

}

object DataportenAuthenticator {
  val userInfoJsonKey = "user"

  val unexpectedResponseCode = s"Unexpected response code from dataporten: %i"
  val unableToParse          = s"Unable to parse UserInfo from dataporten response:\n%s"
}
