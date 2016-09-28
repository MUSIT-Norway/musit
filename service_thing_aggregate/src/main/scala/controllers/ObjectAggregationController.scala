package controllers

import com.google.inject.Inject
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{ ObjectAggregationService, StorageNodeService }

import scala.concurrent.Future

class ObjectAggregationController @Inject() (
    service: ObjectAggregationService,
    storageNodeService: StorageNodeService
) extends Controller {

  def getObjects(mid: Int, nodeId: Long) = Action.async { request =>
    storageNodeService.nodeExists(mid, nodeId).flatMap {
      case MusitSuccess(true) => getObjectsByNodeId(mid, nodeId)
      case MusitSuccess(false) => Future.successful(NotFound(s"Did not find node in museum $mid with nodeId $nodeId"))
      case MusitDbError(msg, ex) =>
        Logger.error(msg, ex.orNull)
        Future.successful(InternalServerError(msg))
      case r: MusitError => Future.successful(InternalServerError(r.message))
    }
  }

  private def getObjectsByNodeId(mid: Int, nodeId: Long): Future[Result] = {
    service.getObjects(mid, nodeId).map {
      case MusitSuccess(objects) =>
        Ok(Json.toJson(objects))
      case MusitDbError(msg, ex) =>
        Logger.error(msg, ex.orNull)
        InternalServerError(msg)
      case r: MusitError =>
        InternalServerError(r.message)
    }
  }
}
