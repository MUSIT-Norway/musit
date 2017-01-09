/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.security.dataporten

import com.google.inject.Inject
import net.ceedubs.ficus.Ficus._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.models.Email
import no.uio.musit.security._
import no.uio.musit.security.dataporten.DataportenAuthenticator._
import no.uio.musit.security.oauth2.{OAuth2Constants, OAuth2Info}
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.libs.ws.{WSAPI, WSResponse}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.api.{Configuration, Logger}

import scala.concurrent.Future

/**
 * Service for communicating with Dataporten
 *
 * @param conf         The Play! Configuration instance
 * @param authResolver Instance for resolving a users groups
 * @param ws           Play! WebService client
 */
class DataportenAuthenticator @Inject() (
    conf: Configuration,
    authResolver: AuthResolver,
    ws: WSAPI
) extends Authenticator with OAuth2Constants {

  private val logger = Logger(classOf[DataportenAuthenticator])

  // Reading in necessary OAuth2 configs
  val authUrl = conf.underlying.as[String](authUrlConfKey)
  val tokenUrl = conf.underlying.as[String](tokenUrlConfKey)
  val callbackUrl = conf.underlying.as[String](callbackUrlConfKey)
  val userInfoUrl = conf.underlying.as[String](userInfoApiConfKey)
  val clientId = conf.underlying.getAs[String](clientIdConfKey).flatMap { str =>
    ClientId.validate(str).toOption.map(ClientId.apply)
  }
  val clientSecret = conf.underlying.as[String](clientSecretConfKey)

  private def validate[A, B](
    res: WSResponse
  )(f: WSResponse => MusitResult[A]): MusitResult[A] = {
    res.status match {
      case ok: Int if ok == Status.OK =>
        logger.info(s"Request contained a valid bearer token")
        logger.info(s"Validating audience...")
        // If the audience doesn't equal the clientId, the user isn't authorized
        val audience = (res.json \ "audience").as[ClientId]
        val usr = (res.json \ "user" \ "userid").as[String]
        if (clientId.contains(audience)) {
          f(res)
        } else {
          logger.warn(s"Access attempt with wrong clientId $audience by user $usr")
          MusitNotAuthorized()
        }

      case ko: Int if ko == Status.UNAUTHORIZED =>
        logger.info(s"Received a request without a valid bearer token.")
        MusitNotAuthenticated()

      case ko: Int =>
        logger.error(unexpectedResponseCode.format(ko))
        MusitInternalError(unexpectedResponseCode.format(ko))
    }
  }

  private type AuthResponse = Future[Either[Result, UserSession]]

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
      ClientID -> Seq(clientId.map(_.asString).getOrElse("")),
      ClientSecret -> Seq(clientSecret),
      GrantType -> Seq(AuthorizationCode),
      Code -> Seq(oauthCode),
      RedirectURI -> Seq(callbackUrl)
    )

    ws.url(tokenUrl).post(params).map { response =>
      logger.debug(s"Access token response from Dataporten: ${response.body}")
      response.json.validate[OAuth2Info] match {
        case err: JsError =>
          val msg = "Invalid JSON response from Dataporten"
          logger.warn(s"$msg: ${Json.prettyPrint(JsError.toJson(err))}")
          Left(Results.InternalServerError(Json.obj("message" -> msg)))

        case JsSuccess(oi, _) =>
          logger.debug("Successfully retrieved an access token from Dataporten")
          Right(oi)
      }
    }
  }

  /**
   * Initialize a new persistent user session.
   */
  private def initSession(): AuthResponse = {
    logger.debug("Initializing OAuth2 process with Dataporten")
    authResolver.sessionInit().map {
      case MusitSuccess(sessionId) =>
        // Set the request params for the Dataporten authorization service.
        val params = Map(
          ClientID -> Seq(clientId.map(_.asString).getOrElse("")),
          RedirectURI -> Seq(callbackUrl),
          ResponseType -> Seq(Code),
          // Note that the OAuth2 "state" parameter is set to the sessionId
          // that was assigned when initializing the session. This allows
          // the state in subsequent callbacks to be validated against the
          // stored value.
          State -> Seq(sessionId.asString)
        )

        logger.trace(s"Using auth URL: $authUrl with params " +
          s"${params.map(p => s"${p._1}=${p._2.head}").mkString("?", "&", "")}")

        Left(Results.Redirect(authUrl, params))

      case err: MusitError =>
        Left(Results.Unauthorized)
    }
  }

  /**
   * Helper function to process the authorization response from Dataporten.
   */
  private def handleAuthResponse[A](
    f: String => AuthResponse
  )(implicit req: Request[A]): AuthResponse = {
    // First check if request contains an Error
    extractParam(Error).map {
      case AccessDenied => Left(Results.Unauthorized)
      case e => Left(Results.Unauthorized(Json.obj("message" -> e)))
    }.map(Future.successful).getOrElse {
      logger.debug(s"Request headers: ${req.headers.toMap.mkString("\n", "\n", "")}")
      logger.debug(s"Request params: ${req.queryString.mkString("\n", "\n", "")}")
      // Now check if the request contains a Code, call service to get token.
      // If there is no code, this is the first step in the OAuth2 flow.
      extractParam(Code) match {
        case Some(code) => f(code)
        case None => initSession()
      }
    }
  }

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
   *    "code" to the configured callback URL. In our case the authentication
   *    endpoint.
   *
   * 3. We then attempt to extract the given "code", and use it to request a new
   *    "access token" from the provider. Which is received in an OAuth2Info
   *    response.
   *
   * 4. Once we have the "access token" we try to fetch the user info from the
   *    provider.
   *
   * 5. With both the OAuth2Info and UserInfo, we can now update the user session
   *    with the information we've received.
   *
   * 6. We can finally return our generated SessionUUID as the token clients
   *    should use as the Bearer token in the HTTP Authorization header.
   *
   * If any single one of the steps above should fail, the process will result
   * in an "Unauthorized" response.
   *
   * @param req The current request.
   * @tparam A The type of the request body.
   * @return Either a Result or the active UserSession
   */
  override def authenticate[A]()(implicit req: Request[A]): AuthResponse =
    handleAuthResponse { code =>
      logger.debug(s"Got code $code. Trying to fetch access token from Dataporten...")
      getToken(code).flatMap {
        case Right(oauthInfo) =>
          // Extract the OAuth2 state from the request
          extractParam(State).flatMap(s => SessionUUID.validate(s).toOption).map { sid =>
            val procRes = for {
              maybeSession <- MusitResultT(authResolver.userSession(sid))
              userInfo <- MusitResultT(userInfo(oauthInfo.accessToken))
              _ <- MusitResultT(authResolver.saveUserInfo(userInfo))
            } yield maybeSession

            procRes.value.flatMap {
              case MusitSuccess(maybeSession) =>
                maybeSession.map { session =>
                  // Update the user session with the Oauth2Info and UserInfo.
                  authResolver.updateSession(session).map {
                    case MusitSuccess(()) => Right(session)
                    case err: MusitError =>
                      logger.error(err.message)
                      Left(Results.Unauthorized)
                  }
                }.getOrElse {
                  logger.error(s"The OAuth2 state $sid did not match any " +
                    s"initialised sessions. This could indicate attempts to spoof" +
                    s"the OAuth2 process.")
                  Future.successful(Left(Results.Unauthorized))
                }

              case err: MusitError =>
                logger.error(err.message)
                Future.successful(Left(Results.Unauthorized))

            }
          }.getOrElse {
            logger.error("Bad state value received from Dataporten. This could "
              + "indicate attempts to spoof the OAuth2 process.")
            Future.successful(Left(Results.Unauthorized))
          }

        case Left(res) => Future.successful(Left(res))
      }
    }

  /**
   * Retrieve the UserInfo from the Dataporten OAuth2 service.
   *
   * TODO: Token should be SessionUUID and session should be looked up.
   * TODO: Need to fetch dataporten token to call userInfo
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  override def userInfo(token: BearerToken): Future[MusitResult[UserInfo]] = {
    ws.url(userInfoUrl).withHeaders(token.asHeader).get().map { response =>
      validate(response) { res =>
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
          Email.fromString(sid).map { email =>
            authResolver.findGroupInfoByFeideEmail(email).map(_.getOrElse(Seq.empty))
          }.getOrElse(Future.successful(Seq.empty))
        }
      }.map(t => MusitSuccess(t.flatten))
    }.getOrElse {
      Future.successful(MusitSuccess(Seq.empty))
    }
  }

}

object DataportenAuthenticator {
  val authUrlConfKey = "musit.dataporten.authorizationURL"
  val tokenUrlConfKey = "musit.dataporten.accessTokenURL"
  val userInfoApiConfKey = "musit.dataporten.userApiURL"
  val callbackUrlConfKey = "musit.dataporten.callbackURL"

  val clientIdConfKey = "musit.dataporten.clientId"
  val clientSecretConfKey = "musit.dataporten.clientSecret"

  val userInfoJsonKey = "user"

  val unexpectedResponseCode = s"Unexpected response code from dataporten: %i"
  val unableToParse = s"Unable to parse UserInfo from dataporten response:\n%s"
}
