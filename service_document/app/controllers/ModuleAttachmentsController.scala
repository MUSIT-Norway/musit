package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document.{ArchiveAddContext, ArchiveContext, ArchiveDocument}
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, CollectionManagement}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import services.DocumentArchiveService

import scala.concurrent.Future.{successful => evaluated}

@Singleton
class ModuleAttachmentsController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val docService: DocumentArchiveService
) extends MusitController {

  private val log = Logger(classOf[ModuleAttachmentsController])

  // ---------------------------------------------------------------------------
  // Attachments for Analysis Module
  // ---------------------------------------------------------------------------

  def uploadAnalysisResult(mid: Int, collectionId: String) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.multipartFormData) {
      implicit request =>
        val colId = Collection.fromString(collectionId).uuid
        if (request.user.canAccess(mid, CollectionManagement, Some(colId))) {
          implicit val ctx = ArchiveAddContext(request.user, mid, colId)

          request.body.files.headOption.map { tmp =>
            ArchiveDocument(
              title = tmp.filename,
              fileType = tmp.contentType,
              stream = Option(FileIO.fromPath(tmp.ref.path))
            )
          }.map { ad =>

            ???
          }.getOrElse(evaluated(BadRequest(Json.obj("message" -> s"No attached file"))))
          ???
        } else {
          evaluated(Forbidden(Json.obj("message" -> s"Unauthorized access")))
        }
    }

  def getFilesForAnalysisResult(mid: Int, fileIds: Seq[String]) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>


      ???
    }

  def downloadAnalysisResult(mid: Int, fileId: String) = {
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getArchiveDocument(fileId).map { res =>
        respond(res) { file =>
          file.stream.map { source =>
            Ok.chunked(source)
              .withHeaders(CONTENT_DISPOSITION -> ContentDisposition(file.title))
          }.getOrElse {
            NotFound(Json.obj("message" -> s"Could not find physical file for $fileId"))
          }
        }
      }

    }
  }

}
