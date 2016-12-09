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
import no.uio.musit.models.Email
import no.uio.musit.security._
import no.uio.musit.security.dataporten.DataportenAuthenticator._
import no.uio.musit.MusitResults._
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.libs.ws.{WSAPI, WSResponse}
import play.api.{Configuration, Logger}

import scala.concurrent.Future

/**
 * Service for communicating with Dataporten
 *
 * TODO: Ensure use of caching of tokens and user/group info
 *
 * @param conf The Play! Configuration instance
 * @param authResolver Instance for resolving a users groups
 * @param ws Play! WebService client
 */
class DataportenAuthenticator @Inject() (
    conf: Configuration,
    authResolver: AuthResolver,
    ws: WSAPI
) extends Authenticator {

  private val logger = Logger(classOf[DataportenAuthenticator])

  val userInfoUrl = conf.underlying.as[String](userApiConfKey)
  val clientId = conf.underlying.getAs[String](clientIdConfKey).flatMap { str =>
    ClientId.validate(str).toOption.map(ClientId.apply)
  }

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

  /**
   * Retrieve the UserInfo from the Dataporten OAuth service.
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
            // If the user doesn't exist, we add it to the UserInfo table
            authResolver.saveUserInfo(userInfo).foreach {
              case MusitSuccess(()) => logger.debug("Successfully authenticated user")
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
  override def groups(userInfo: UserInfo): Future[Seq[GroupInfo]] = {

    def stripPrefix(s: String): String = s.reverse.takeWhile(_ != ':').reverse.trim

    userInfo.secondaryIds.map { sids =>
      Future.sequence {
        sids.map(stripPrefix).filter(_.contains("@")).map { sid =>
          Email.fromString(sid).map { email =>
            authResolver.findGroupInfoByFeideEmail(email).map(_.getOrElse(Seq.empty))
          }.getOrElse(Future.successful(Seq.empty))
        }
      }.map(_.flatten)
    }.getOrElse {
      Future.successful(Seq.empty)
    }
  }
}

object DataportenAuthenticator {
  val userApiConfKey = "musit.dataporten.userApiURL"
  val userInfoJsonKey = "user"

  val clientIdConfKey = "musit.dataporten.clientId"

  val unexpectedResponseCode = s"Unexpected response code from dataporten: %i"
  val unableToParse = s"Unable to parse UserInfo from dataporten response:\n%s"
}
