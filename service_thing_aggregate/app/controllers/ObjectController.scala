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
import no.uio.musit.MusitResults.{MusitDbError, MusitError, MusitSuccess}
import no.uio.musit.models.{MuseumId, MuseumNo, ObjectUUID, SubNo}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.{Configuration, Logger}
import services.{ObjectService, StorageNodeService}

import scala.concurrent.Future

class ObjectController @Inject()(
    val authService: Authenticator,
    val conf: Configuration,
    val objService: ObjectService,
    val nodeService: StorageNodeService
) extends MusitController {

  val maxLimitConfKey     = "musit.objects.search.max-limit"
  val defaultLimitConfKey = "musit.objects.search.default-limit"

  val logger = Logger(classOf[ObjectController])

  private val maxLimit     = conf.getInt(maxLimitConfKey).getOrElse(100)
  private val defaultLimit = conf.getInt(defaultLimitConfKey).getOrElse(25)

  private def calcLimit(l: Int): Int = l match {
    case lim: Int if lim > maxLimit => maxLimit
    case lim: Int if lim < 0        => defaultLimit
    case lim: Int                   => lim
  }

  /**
   * Controller enabling searching for objects. It has 3 search specific fields
   * that may or may not contain different criteria. There are also fields to
   * specify paging and a limit for how many results should be returned.
   *
   * @param mid           the MuseumId to filter on
   * @param collectionIds Comma separated String of CollectionUUIDs.
   * @param page          the page number to return
   * @param limit         number of results per page
   * @param museumNo      museum number to search for
   * @param subNo         museum sub-number to search for
   * @param term          the object term to search for
   * @return A JSON containing the objects that were found.
   */
  def search(
      mid: Int,
      collectionIds: String,
      page: Int,
      limit: Int = defaultLimit,
      museumNo: Option[String],
      subNo: Option[String],
      term: Option[String]
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    parseCollectionIdsParam(mid, collectionIds)(request.user) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        if (museumNo.isEmpty && subNo.isEmpty && term.isEmpty) {
          Future.successful(
            BadRequest(
              Json.obj(
                "messages" -> "at least one of museumNo, subNo or term must be specified."
              )
            )
          )
        } else {
          val mno = museumNo.map(MuseumNo.apply)
          val sno = subNo.map(SubNo.apply)
          val lim = calcLimit(limit)

          objService.search(mid, cids, page, lim, mno, sno, term)(request.user).map {
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

  /**
   * Endpoint to fetch objects that share the same main object ID.
   *
   * @param mid          The MuseumId to look for objects in.
   * @param mainObjectId The main object ID to look for.
   * @return A list of objects that share the same main object ID.
   */
  def findMainObjectChildren(
      mid: Int,
      mainObjectId: Long,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    parseCollectionIdsParam(mid, collectionIds)(request.user) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        objService.findMainObjectChildren(mid, mainObjectId, cids)(request.user).map {
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
  }

  /**
   * Endpoint that will retrieve objects for a given nodeId in a museum. The
   * result is paged, so that only the given {{{limit}}} of results are
   * returned to the client.
   *
   * @param mid           The MuseumId to look for the objects and node.
   * @param nodeId        The StorageNodeDatabaseId to get objects for
   * @param collectionIds Comma separated String of CollectionUUIDs.
   * @param page          The resultset page number.
   * @param limit         The number of results per page.
   * @return A list of objects located in the given node.
   */
  def getObjects(
      mid: Int,
      nodeId: Long,
      collectionIds: String,
      page: Int,
      limit: Int = defaultLimit
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    parseCollectionIdsParam(mid, collectionIds)(request.user) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        nodeService.nodeExists(mid, nodeId).flatMap {
          case MusitSuccess(true) =>
            objService.findObjects(mid, nodeId, cids, page, limit)(request.user).map {
              case MusitSuccess(pagedObjects) =>
                Ok(
                  Json.obj(
                    "totalMatches" -> pagedObjects.totalMatches,
                    "matches"      -> Json.toJson(pagedObjects.matches)
                  )
                )

              case MusitDbError(msg, ex) =>
                logger.error(msg, ex.orNull)
                InternalServerError(Json.obj("message" -> msg))

              case r: MusitError =>
                InternalServerError(Json.obj("message" -> r.message))
            }

          case MusitSuccess(false) =>
            Future.successful(
              NotFound(
                Json.obj(
                  "message" -> s"Did not find node in museum $mid with nodeId $nodeId"
                )
              )
            )

          case MusitDbError(msg, ex) =>
            logger.error(msg, ex.orNull)
            Future.successful(InternalServerError(Json.obj("message" -> msg)))

          case r: MusitError =>
            Future.successful(InternalServerError(Json.obj("message" -> r.message)))
        }
    }
  }

  def scanForOldBarcode(
      mid: MuseumId,
      oldBarcode: Long,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { request =>
    parseCollectionIdsParam(mid, collectionIds)(request.user) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        objService.findByOldBarcode(mid, oldBarcode, cids)(request.user).map {
          case MusitSuccess(objects) =>
            Ok(Json.toJson(objects))

          case MusitDbError(msg, ex) =>
            logger.error(msg, ex.orNull)
            InternalServerError(Json.obj("message" -> msg))

          case r: MusitError =>
            InternalServerError(Json.obj("message" -> r.message))
        }
    }
  }

  def findObjectByUUID(
    mid: MuseumId,
    objectUUID: String,
    collectionIds: String
  ) = MusitSecureAction(mid, Read).async { request =>
    parseCollectionIdsParam(mid, collectionIds)(request.user) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        ObjectUUID
          .fromString(objectUUID)
          .map { uuid =>
            objService.findByUUID(mid, uuid, cids)(request.user).map {
              case MusitSuccess(objects) =>
                Ok(Json.toJson(objects))

              case MusitDbError(msg, ex) =>
                logger.error(msg, ex.orNull)
                InternalServerError(Json.obj("message" -> msg))

              case r: MusitError =>
                InternalServerError(Json.obj("message" -> r.message))
            }
          }.getOrElse(
            Future.successful(
              BadRequest(Json.obj("message" -> s"Invalid object UUID $objectUUID"))
            )
          )
    }
  }
}
