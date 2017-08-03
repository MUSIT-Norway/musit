package controllers.musitobject

import com.google.inject.Inject
import controllers._
import models.musitobject.ObjectSearchResult
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import services.musitobject.ObjectService
import services.storage.StorageNodeService

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
    implicit val currUser = request.user

    parseCollectionIdsParam(mid, collectionIds) match {
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

          objService.search(mid, cids, page, lim, mno, sno, term).map {
            case MusitSuccess(res) =>
              Ok(Json.toJson[ObjectSearchResult](res))

            case err: MusitError =>
              logger.error(err.message)
              internalErr(err)
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
      mainObjectId: String,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    ObjectUUID
      .fromString(mainObjectId)
      .map { oid =>
        parseCollectionIdsParam(mid, collectionIds) match {
          case Left(res) => Future.successful(res)
          case Right(cids) =>
            objService.findMainObjectChildren(mid, oid, cids).map {
              case MusitSuccess(res) =>
                Ok(Json.toJson(res))

              case err: MusitError =>
                logger.error(err.message)
                internalErr(err)
            }
        }
      }
      .getOrElse(invaludUuidResponse(mainObjectId))
  }

  // scalastyle:off method.length
  /**
   * Endpoint that will retrieve objects for a given nodeId in a museum. The
   * result is paged, so that only the given {{{limit}}} of results are
   * returned to the client.
   *
   * @param mid           The MuseumId to look for the objects and node.
   * @param nodeId        The StorageNodeId to get objects for
   * @param collectionIds Comma separated String of CollectionUUIDs.
   * @param page          The result set page number.
   * @param limit         The number of results per page.
   * @return A list of objects located in the given node.
   */
  def getObjects(
      mid: Int,
      nodeId: String,
      collectionIds: String,
      page: Int,
      limit: Int = defaultLimit
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    implicit val currUser = request.user

    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        parseCollectionIdsParam(mid, collectionIds) match {
          case Left(res) => Future.successful(res)
          case Right(cids) =>
            nodeService.exists(mid, nid).flatMap {
              case MusitSuccess(true) =>
                objService.findObjects(mid, nid, cids, page, limit).map {
                  case MusitSuccess(pagedObjects) =>
                    Ok(
                      Json.obj(
                        "totalMatches" -> pagedObjects.totalMatches,
                        "matches"      -> Json.toJson(pagedObjects.matches)
                      )
                    )

                  case r: MusitError =>
                    internalErr(r)
                }

              case MusitSuccess(false) =>
                Future.successful(
                  NotFound(
                    Json.obj(
                      "message" -> s"Did not find node in museum $mid with nodeId $nodeId"
                    )
                  )
                )

              case r: MusitError =>
                logger.error(r.message)
                Future.successful(internalErr(r))
            }
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  // scalastyle:on method.length

  def scanForOldBarcode(
      mid: MuseumId,
      oldBarcode: Long,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { request =>
    implicit val currUser = request.user

    parseCollectionIdsParam(mid, collectionIds) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        objService.findByOldBarcode(mid, oldBarcode, cids).map {
          case MusitSuccess(objects) =>
            Ok(Json.toJson(objects))

          case r: MusitError =>
            logger.error(r.message)
            internalErr(r)
        }
    }
  }

  def findObjectByUUID(
      mid: MuseumId,
      objectUUID: String,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { request =>
    implicit val currUser = request.user

    parseCollectionIdsParam(mid, collectionIds) match {
      case Left(res) => Future.successful(res)
      case Right(cids) =>
        ObjectUUID
          .fromString(objectUUID)
          .map { uuid =>
            objService.findByUUID(mid, uuid, cids).map {
              case MusitSuccess(maybeObject) =>
                maybeObject.fold(
                  NotFound(
                    Json.obj("message" -> s"Did not find object UUID $objectUUID")
                  )
                )(obj => Ok(Json.toJson(obj)))

              case r: MusitError =>
                logger.error(r.message)
                internalErr(r)
            }
          }
          .getOrElse(invaludUuidResponse(objectUUID))
    }
  }
}
