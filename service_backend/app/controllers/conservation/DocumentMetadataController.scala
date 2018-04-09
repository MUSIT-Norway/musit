package controllers.conservation

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.functional.Extensions._
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.security.{Authenticator, CollectionManagement, DocumentArchive}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.{ControllerComponents, Result}
import services.conservation.DocumentMetadataService
import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitHttpError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._
import services.geolocation.GeoLocationService
import play.api.mvc.ControllerComponents

import scala.util.control.NonFatal
@Singleton
class DocumentMetadataController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val documentMetadataService: DocumentMetadataService
) extends MusitController {

  val logger = Logger(classOf[DocumentMetadataController])

  def getFilename(mid: MuseumId, fileId: String) = {
    println("Reached DocumentMetadataController.getFilename")
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      println("DocumentMetadataController request.token: " + request.token)
      implicit val currUser = request.user
      println("Reached DocumentMetadataController.getFilename implicit request... ")
      documentMetadataService
        .getFilename(mid, fileId, currUser)
        .map {
          case MusitSuccess(locations) =>
            Ok(Json.toJson(locations))

          case MusitHttpError(status, msg) =>
            Status(status)

          case err: MusitError =>
            logger.error("InternalServerError: " + s"${err.message}")
            InternalServerError(Json.obj("message" -> err.message))
        }
        .recover {
          case NonFatal(ex) =>
            val msg = "An error occurred when searching for filename"
            logger.error(msg, ex)
            InternalServerError(Json.obj("message" -> msg))
        }
    }
  }
}
