package controllers

import com.google.inject.Inject
import models.{Museum, MuseumId}
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{ObjectAggregationService, StorageNodeService}

import scala.concurrent.Future

class ObjectAggregationController @Inject() (
    service: ObjectAggregationService,
    storageNodeService: StorageNodeService
) extends Controller {

  def getObjects(mid: Int, nodeId: Long) = Action.async { request =>
    Museum.fromMuseumId(mid).map { museumId =>
      storageNodeService.nodeExists(mid, nodeId).flatMap {
        case MusitSuccess(true) =>
          getObjectsByNodeId(mid, nodeId)

        case MusitSuccess(false) =>
          Future.successful(
            NotFound(Json.obj("message" -> s"Did not find node in museum $mid with nodeId $nodeId"))
          )

        case MusitDbError(msg, ex) =>
          Logger.error(msg, ex.orNull)
          Future.successful(InternalServerError(Json.obj("message" -> msg)))

        case r: MusitError =>
          Future.successful(InternalServerError(Json.obj("message" -> r.message)))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  private def getObjectsByNodeId(mid: MuseumId, nodeId: Long): Future[Result] = {
    service.getObjects(mid, nodeId).map {
      case MusitSuccess(objects) =>
        Ok(Json.toJson(objects))

      case MusitDbError(msg, ex) =>
        Logger.error(msg, ex.orNull)
        InternalServerError(Json.obj("message" -> msg))

      case r: MusitError =>
        InternalServerError(Json.obj("message" -> r.message))
    }
  }

}
