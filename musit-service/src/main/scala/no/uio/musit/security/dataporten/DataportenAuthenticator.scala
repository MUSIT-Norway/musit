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

  private def extractParam[A](param: String)(implicit req: Request[A]): Option[String] = {
    req.queryString.get(param).flatMap(_.headOption)
  }

  private def fetchToken(
    code: String
  )(implicit req: RequestHeader): Future[Either[Result, OAuth2Info]] = {
    val params = Map(
      ClientID -> Seq(clientId.map(_.asString).getOrElse("")),
      ClientSecret -> Seq(clientSecret),
      GrantType -> Seq(AuthorizationCode),
      Code -> Seq(code),
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
   * Starts the OAuth2 authentication process.
   *
   * @param req The current request.
   * @tparam A The type of the request body.
   * @return Either a Result or the OAuth2Info from Dataporten
   */
  // scalastyle:off method.length
  override def authenticate[A]()(
    implicit
    req: Request[A]
  ): Future[Either[Result, OAuth2Info]] = {
    // 1. Check if request contains an Error
    extractParam(Error).map {
      case AccessDenied => Left(Results.Unauthorized)
      case e => Left(Results.Unauthorized(Json.obj("message" -> e)))
    }.map(Future.successful).getOrElse {

      logger.debug(s"Request headers: ${req.headers.toMap.mkString("\n", "\n", "")}")
      logger.debug(s"Request params: ${req.queryString.mkString("\n", "\n", "")}")

      extractParam(Code) match {
        // 2. If the request contains a Code, call service to get token.
        case Some(code) =>
          logger.debug(s"Got code $code. Trying to fetch access token from Dataporten...")
          fetchToken(code).map { resultOrInfo =>
            resultOrInfo.right.foreach { oi =>
              userInfo(oi.accessToken).map {
                case MusitSuccess(ui) => ???
                case err: MusitError => ???
              }
            }
            resultOrInfo
          }

        // TODO: get Dataporten UserInfo and save if not exists
        // TODO: prepare session

        case None =>
          // 3. If none of the above, this is the first step in the OAuth2 flow.
          logger.debug("Initializing OAuth2 process with Dataporten")

          authResolver.sessionInit().map {
            case MusitSuccess(sessionId) =>
              // Set the request params for the Dataporten authorization service
              // Note that the OAuth2 "state" parameter is set to thesessionId
              // that was assigned when initializing the session. This allows
              // the state in subsequent callbacks to be validated against the
              // stored value.
              val params = Map(
                ClientID -> Seq(clientId.map(_.asString).getOrElse("")),
                RedirectURI -> Seq(callbackUrl),
                ResponseType -> Seq(Code),
                State -> Seq(sessionId.asString)
              )

              logger.trace(s"Using auth URL: $authUrl with params " +
                s"${params.map(p => s"${p._1}=${p._2.head}").mkString("?", "&", "")}")

              Left(Results.Redirect(authUrl, params))

            case err: MusitError =>
              Left(Results.Unauthorized)
          }
      }
    }
  }
  // scalastyle:on method.length

  /**
   * Retrieve the UserInfo from the Dataporten OAuth2 service.
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  override def userInfo(token: BearerToken): Future[MusitResult[UserInfo]] = {
    /*
      TODO: This method should be modified to _first_ look for the UserInfo
      associated the incoming token. If there is no match, or the token is
      no longer valid, call the Dataporten userInfo service.

      Flow will then be:

      1. Check authResolver for token <=> user info match
      2. IFF not found call Dataporten and Save UserInfo
      3. Return UserInfo
     */
    ws.url(userInfoUrl).withHeaders(token.asHeader).get().map { response =>
      validate(response) { res =>
        // The user info part of the message is always under the "user" key.
        // So we get it explicitly to deserialize to an UserInfo instance.
        val usrInfoJson = (response.json \ userInfoJsonKey).as[JsObject]
        usrInfoJson.validate[UserInfo] match {
          case JsSuccess(userInfo, _) =>
            // If the user doesn't exist, we add it to the UserInfo table
            authResolver.saveUserInfo(userInfo).foreach {
              case MusitSuccess(()) => logger.debug("Successfully saved UserInfo")
              case err: MusitError => logger.debug(err.message)
            }
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
