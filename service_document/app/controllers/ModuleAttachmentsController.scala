package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document._
import net.scalytica.symbiotic.api.types.FileId
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.functional.Implicits.futureMonad
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
            val dest = BaseFolders.AnalysisFolder.path(mid)

            val res = for {
              a <- MusitResultT(docService.saveArchiveDocument(dest, ad))
              b <- MusitResultT(docService.getArchiveDocument(a)(ctx))
            } yield b

            res.value.map {
              case MusitSuccess(added) =>
                Ok(Json.toJson(added))

              case err: MusitError =>
                InternalServerError(Json.obj("message" -> s"${err.message}"))
            }
          }.getOrElse(evaluated(BadRequest(Json.obj("message" -> s"No attached file"))))
        } else {
          evaluated(Forbidden(Json.obj("message" -> s"Unauthorized access")))
        }
    }

  def getFilesForAnalysisResult(mid: Int, fileIds: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      val fids = fileIds.split(",").map(FileId.apply)

      val res = MusitResultT.sequenceF {
        fids.map(fid => docService.getArchiveDocument(fid))
      }

      res.value.map {
        case MusitSuccess(docs) =>
          Ok(Json.toJson[Seq[ArchiveDocumentItem]](docs))

        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
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
