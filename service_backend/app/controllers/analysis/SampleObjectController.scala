package controllers.analysis

import com.google.inject.{Inject, Singleton}
import controllers._
import models.analysis.{SampleObject, SaveSampleObject}
import no.uio.musit.MusitResults._
import no.uio.musit.models.{MuseumId, ObjectUUID, StorageNodeId}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{ControllerComponents, Result}
import services.analysis.SampleObjectService
import services.storage.StorageNodeService

import scala.concurrent.Future

@Singleton
class SampleObjectController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val soService: SampleObjectService,
    val nodeService: StorageNodeService
) extends MusitController {

  val logger = Logger(classOf[SampleObjectController])

  private def get[A](
      find: => Future[MusitResult[A]]
  )(
      jsConvert: A => Result
  ): Future[Result] = {
    find.map {
      case MusitSuccess(res) => jsConvert(res)
      case err: MusitError   => internalErr(err)
    }
  }

  def getById(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = request.user

      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          get[Option[SampleObject]](soService.findById(oid)) { maybeObject =>
            maybeObject.map(so => Ok(Json.toJson(so))).getOrElse(NotFound)
          }
        }
        .getOrElse(invaludUuidResponse(uuid))
    }

  def getForMuseum(mid: MuseumId) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = request.user

      get[Seq[SampleObject]](soService.findForMuseum(mid))(
        listAsPlayResult
      )
    }

  def getForParentObject(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = request.user

      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          get[Seq[SampleObject]](soService.findForParent(oid))(
            listAsPlayResult
          )
        }
        .getOrElse(invaludUuidResponse(uuid))
    }

  def getForOriginatingObject(mid: MuseumId, uuid: String) =
    MusitSecureAction().async { implicit request =>
      implicit val currUser = request.user

      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          get[Seq[SampleObject]](soService.findForOriginating(oid))(
            listAsPlayResult
          )
        }
        .getOrElse(invaludUuidResponse(uuid))
    }

  def getSamplesForNode(
      mid: Int,
      nodeId: String,
      collectionIds: String
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
                soService.findForNode(mid, nid, cids).map {
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
      implicit val currUser = request.user

      saveRequest(request.body.validate[SaveSampleObject]) { cso =>
        soService.add(mid, cso.asSampleObject)
      }
    }

  def update(mid: MuseumId, uuid: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val currUser = request.user

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
      implicit val currUser = request.user

      ObjectUUID
        .fromString(uuid)
        .map { oid =>
          soService.delete(oid).map {
            case MusitSuccess(_)  => Ok
            case MusitNotFound(_) => NotFound
            case err: MusitError  => internalErr(err)
          }
        }
        .getOrElse(invaludUuidResponse(uuid))
    }
}
