package controllers.delphi

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import services.musitobject.ObjectService
import services.storage.StorageNodeService

import scala.concurrent.Future

class DelphiBridgeController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val nodeService: StorageNodeService,
    val objService: ObjectService
) extends MusitController {

  val logger = Logger(classOf[DelphiBridgeController])

  /**
   * Returns the StorageNodeDatabaseId and name for an objects current location.
   */
  def currentNode(
      oldObjectId: Long,
      schemaName: String
  ) = MusitSecureAction().async { implicit request =>
    implicit val currUser = request.user

    nodeService.currNodeForOldObject(oldObjectId, schemaName).map {
      case MusitSuccess(mres) =>
        mres.map { res =>
          Ok(
            Json.obj(
              "nodeId"          -> Json.toJson(res._1),
              "currentLocation" -> res._2
            )
          )
        }.getOrElse {
          Ok(
            Json.obj(
              "nodeId"          -> "",
              "currentLocation" -> s"Gjenstanden har ingen plassering."
            )
          )
        }

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Endpoint that returns all the nodes under a museums external root nodes.
   */
  def outsideNodes(mid: Int) = MusitSecureAction(Read).async { implicit request =>
    implicit val currUser = request.user

    nodeService.nodesOutsideMuseum(mid).map {
      case MusitSuccess(res) =>
        val jsSeq = JsArray(
          res.map(
            sn =>
              Json.obj(
                "nodeId" -> Json.toJson(sn._1),
                "name"   -> sn._2
            )
          )
        )
        Ok(jsSeq)

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  case class TranslateIdRequest(schemaName: String, oldObjectIds: Seq[Long])

  object TranslateIdRequest {
    implicit val reads: Reads[TranslateIdRequest] = Json.reads[TranslateIdRequest]
  }

  /**
   * Endpoint for converting old object IDs from the old MUSIT database schemas
   * to an objectId recognized by the new system.
   */
  def translateOldObjectIds = MusitSecureAction().async(parse.json) { implicit request =>
    implicit val currUser = request.user

    request.body.validate[TranslateIdRequest] match {
      case JsSuccess(trans, _) =>
        objService.findByOldObjectIds(trans.schemaName, trans.oldObjectIds).map {
          case MusitSuccess(res) =>
            Ok(Json.toJson(res))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }

      case jsErr: JsError =>
        Future.successful(BadRequest(JsError.toJson(jsErr)))
    }
  }

}
