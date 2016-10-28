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

package no.uio.musit.security

import com.google.inject.Inject
import net.ceedubs.ficus.Ficus._
import no.uio.musit.security.DataportenAuthenticator._
import no.uio.musit.service.MusitResults.{MusitInternalError, MusitNotAuthenticated, MusitResult, MusitSuccess}
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.libs.ws.{WSAPI, WSResponse}
import play.api.{Configuration, Logger}

import scala.concurrent.Future

/**
 * Service for communicating with Dataporten
 *
 * @param configuration The Play! Configuration instance
 */
class DataportenAuthenticator @Inject() (
    configuration: Configuration,
    ws: WSAPI
) extends Authenticator {

  private val logger = Logger(classOf[DataportenAuthenticator])

  val userInfoUrl = configuration.underlying.as[String](userApiConfKey)

  val groupInfoUrl = configuration.underlying.as[String](groupApiConfKey)

  private def validate[A, B](
    res: WSResponse
  )(f: WSResponse => MusitResult[A]): MusitResult[A] = {
    res.status match {
      case ok: Int if ok == Status.OK =>
        logger.debug(s"Received a request with valid bearer token")
        f(res)

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
   * Retrieve all the GroupInfo, for the user associated with the given token,
   * from the Dataporten OAuth service.
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return a Seq of GroupInfo wrapped in a MusitResult
   */
  override def groups(
    token: BearerToken
  ): Future[MusitResult[Seq[GroupInfo]]] = {
    ws.url(groupInfoUrl).withHeaders(token.asHeader).get().map { response =>
      validate(response) { res =>
        response.json.validate[Seq[GroupInfo]] match {
          case JsSuccess(groups, _) =>
            MusitSuccess(groups)

          case err: JsError =>
            val prettyError = Json.prettyPrint(JsError.toJson(err))
            logger.error(unableToParse.format(prettyError))
            MusitInternalError(unableToParse.format(prettyError))
        }
      }
    }
  }
}

object DataportenAuthenticator {
  val userApiConfKey = "musit.dataporten.userApiURL"
  val groupApiConfKey = "musit.dataporten.groupApiURL"
  val userInfoJsonKey = "user"

  val unexpectedResponseCode = s"Unexpected response code from dataporten: %i"
  val unableToParse = s"Unable to parse UserInfo from dataporten response:\n%s"
}
