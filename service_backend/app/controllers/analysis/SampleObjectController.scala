package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers._
import models.analysis.SaveSampleObject
import no.uio.musit.MusitResults.{MusitEmpty, MusitError, MusitSuccess}
import no.uio.musit.models.{MuseumId, ObjectUUID, StorageNodeId}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.analysis.SampleObjectService
import services.storage.StorageNodeService

import scala.concurrent.Future

@Singleton
class SampleObjectController @Inject()(
    val authService: Authenticator,
    val soService: SampleObjectService,
    val nodeService: StorageNodeService
) extends MusitController {

  val logger = Logger(classOf[SampleObjectController])

  def getForMuseum(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      soService.findForMuseum(mid)(request.user).map {
        case MusitSuccess(objects) => listAsPlayResult(objects)
        case err: MusitError       => internalErr(err)
      }
    }

  def getById(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          soService.findById(oid)(request.user).map {
            case MusitSuccess(maybeObject) =>
              maybeObject.map(so => Ok(Json.toJson(so))).getOrElse(NotFound)

            case err: MusitError => internalErr(err)

          }
        }
        .getOrElse(invaludUuidResponse(uuid))
    }

  def getForParentObject(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          soService.findForParent(oid)(request.user).map {
            case MusitSuccess(objects) => listAsPlayResult(objects)
            case err: MusitError       => internalErr(err)

          }
        }
        .getOrElse(invaludUuidResponse(uuid))
    }

  def getSamplesForNode(
      mid: Int,
      nodeId: String,
      collectionIds: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        parseCollectionIdsParam(mid, collectionIds)(request.user) match {
          case Left(res) => Future.successful(res)
          case Right(cids) =>
            nodeService.exists(mid, nid).flatMap {
              case MusitSuccess(true) =>
                soService.findForNode(mid, nid, cids)(request.user).map {
                  case MusitSuccess(samples) => listAsPlayResult(samples)
                  case err: MusitError       => internalErr(err)
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
                Future.successful(internalErr(r))
            }
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  def save(mid: MuseumId) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)

      saveRequest(request.body.validate[SaveSampleObject]) { cso =>
        soService.add(mid, cso.asSampleObject)
      }
    }

  def update(mid: MuseumId, uuid: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = implicitly(request.user)
      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          request.body.validate[SaveSampleObject] match {
            case JsSuccess(saveSampleObject, _) =>
              soService.update(oid, saveSampleObject.asSampleObject).map {
                case MusitSuccess(mso) =>
                  mso.map(u => Ok(Json.toJson(u))).getOrElse(NotFound)

                case err: MusitError =>
                  internalErr(err)
              }

            case err: JsError =>
              Future.successful(BadRequest(JsError.toJson(err)))
          }
        }
        .getOrElse(invaludUuidResponse(uuid))

    }

  def delete(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = implicitly(request.user)
      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          soService.delete(oid).map {
            case MusitSuccess(_) => Ok
            case MusitEmpty      => NotFound
            case err: MusitError => internalErr(err)
          }
        }
        .getOrElse(invaludUuidResponse(uuid))
    }
}
