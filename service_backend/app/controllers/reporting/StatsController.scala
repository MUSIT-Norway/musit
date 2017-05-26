package controllers.reporting

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.StorageNodeId
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import services.reporting.StatsService

import scala.concurrent.Future

class StatsController @Inject()(
    val authService: Authenticator,
    val service: StatsService
) extends MusitController {

  val logger = Logger(classOf[StatsController])

  /**
   * TODO: Document me!
   */
  def stats(
      mid: Int,
      nodeId: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        service.nodeStats(mid, nid)(request.user).map {
          case MusitSuccess(maybeStats) =>
            maybeStats.map { stats =>
              Ok(Json.toJson(stats))
            }.getOrElse {
              NotFound(Json.obj("message" -> s"Could not find nodeId $nodeId"))
            }

          case err: MusitError =>
            logger.error(
              "An unexpected error occured when trying to read " +
                s"node stats for $nodeId. Message was: ${err.message}"
            )
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"Invalid UUID $nodeId"))
        }
      }
  }

}
