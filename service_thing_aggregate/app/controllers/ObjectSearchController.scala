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

package controllers

import com.google.inject.Inject
import models.ObjectSearchResult
import no.uio.musit.models.{MuseumNo, ObjectId, SubNo}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitDbError, MusitError, MusitSuccess}
import no.uio.musit.security.Permissions.Read
import play.api.{Configuration, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Results
import services.ObjectSearchService

import scala.concurrent.Future

class ObjectSearchController @Inject() (
    val authService: Authenticator,
    val conf: Configuration,
    val service: ObjectSearchService
) extends MusitController {

  val maxLimitConfKey = "musit.objects.search.max-limit"
  val defaultLimitConfKey = "musit.objects.search.default-limit"

  val logger = Logger(classOf[ObjectSearchController])

  private val maxLimit = conf.getInt(maxLimitConfKey).getOrElse(100)
  private val defaultLimit = conf.getInt(defaultLimitConfKey).getOrElse(25)

  private def calcLimit(l: Int): Int = l match {
    case lim: Int if lim > maxLimit => maxLimit
    case lim: Int if lim < 0 => defaultLimit
    case lim: Int => lim
  }

  def getMainObjectChildren(
    mid: Int,
    mainObjectId: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.getMainObjectChildren(mid, ObjectId.fromLong(mainObjectId)).map {
      case MusitSuccess(res) =>
        Ok(Json.toJson(res))
      case MusitDbError(msg, ex) =>
        logger.error(msg, ex.orNull)
        InternalServerError(Json.obj("message" -> msg))
      case err: MusitError =>
        logger.error(err.message)
        Results.InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Controller enabling searching for objects. It has 3 search specific fields
   * that may or may not contain different criteria. There are also fields to
   * specify paging and a limit for how many results should be returned.
   */
  def search(
    mid: Int,
    page: Int = 1,
    limit: Int = defaultLimit,
    museumNo: Option[String],
    subNo: Option[String],
    term: Option[String]
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    if (museumNo.isEmpty && subNo.isEmpty && term.isEmpty) {
      Future.successful {
        BadRequest(Json.obj(
          "messages" -> "at least one of museumNo, subNo or term must be specified"
        ))
      }
    } else {
      val mno = museumNo.map(MuseumNo.apply)
      val sno = subNo.map(SubNo.apply)
      service.search(mid, page, calcLimit(limit), mno, sno, term).map {
        case MusitSuccess(res) =>
          Ok(Json.toJson[ObjectSearchResult](res))

        case MusitDbError(msg, ex) =>
          logger.error(msg, ex.orNull)
          InternalServerError(Json.obj("message" -> msg))

        case err: MusitError =>
          logger.error(err.message)
          Results.InternalServerError(Json.obj("message" -> err.message))
      }
    }
  }
}
