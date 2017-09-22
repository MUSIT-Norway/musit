package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document._
import net.scalytica.symbiotic.api.types.{FileId, FolderId, Path}
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{CollectionUUID, MuseumId}
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, CollectionManagement, ModuleConstraint}
import no.uio.musit.service.{MusitController, MusitRequest}
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, MultipartFormData}
import services.DocumentArchiveService

import scala.concurrent.Future
import scala.concurrent.Future.{successful => evaluated}

@Singleton
class ModuleAttachmentsController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val docService: DocumentArchiveService
) extends MusitController {

  private val log = Logger(classOf[ModuleAttachmentsController])

  // ---------------------------------------------------------------------------
  // Shared logic for module specific endpoints
  // ---------------------------------------------------------------------------

  private def createIfNotExists(
      destPath: Path
  )(implicit ctx: ArchiveAddContext): Future[MusitResult[Option[FolderId]]] = {
    val res = for {
      exists <- MusitResultT(docService.archiveFolderItemPathExists(destPath)(ctx))
      maybeCreated <- {
        if (!exists) MusitResultT(docService.createGenericFolderItemAtPath(destPath))
        else MusitResultT.successful(MusitSuccess[Option[FolderId]](None))
      }
    } yield maybeCreated

    res.value
  }

  private def uploadFile(
      mid: MuseumId,
      dataIdentifier: String,
      colId: CollectionUUID,
      module: ModuleConstraint,
      modulePath: Path
  )(implicit request: MusitRequest[MultipartFormData[Files.TemporaryFile]]) = {
    if (request.user.canAccess(mid, module, Some(colId))) {
      implicit val ctx = ArchiveAddContext(request.user, mid, colId)

      request.body.files.headOption.map { tmp =>
        ArchiveDocument(
          title = tmp.filename,
          fileType = tmp.contentType,
          stream = Option(FileIO.fromPath(tmp.ref.path))
        )
      }.map { ad =>
        val destPath = modulePath.append(dataIdentifier)
        val res = for {
          _ <- MusitResultT(createIfNotExists(destPath))
          a <- MusitResultT(docService.saveArchiveDocument(destPath, ad))
          b <- MusitResultT(docService.getArchiveDocument(a)(ctx))
        } yield b

        res.value.map {
          case MusitSuccess(added) =>
            Created(Json.toJson(added))

          case MusitValidationError(msg, _, _) =>
            BadRequest(Json.obj("message" -> msg))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }.getOrElse {
        evaluated(BadRequest(Json.obj("message" -> s"No attached file")))
      }
    } else {
      evaluated(Forbidden(Json.obj("message" -> s"Unauthorized access")))
    }
  }

  private def metadataForFileIds(fids: Seq[FileId])(implicit ctx: ArchiveContext) = {
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

  private def downloadFile(fid: FileId)(implicit ctx: ArchiveContext) = {
    docService.getArchiveDocument(fid).map { res =>
      respond(res) { file =>
        file.stream.map { source =>
          Ok.chunked(source)
            .withHeaders(CONTENT_DISPOSITION -> ContentDisposition(file.title))
        }.getOrElse {
          NotFound(Json.obj("message" -> s"Could not find physical file for $fid"))
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Attachments for Analysis
  // ---------------------------------------------------------------------------

  def uploadAnalysisResult(mid: Int, analysisId: String, collectionId: String) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.multipartFormData) {
      implicit request =>
        parseCollectionIdParam(collectionId) match {
          case Right(colId) =>
            uploadFile(
              mid = mid,
              dataIdentifier = analysisId,
              colId = colId,
              module = CollectionManagement,
              modulePath = BaseFolders.AnalysisFolder.path(mid)
            )

          case Left(err) => evaluated(err)
        }

    }

  def getFilesForAnalysisResult(mid: Int, fileIds: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      parseFileIdsParam(fileIds) match {
        case Right(fids) => metadataForFileIds(fids)
        case Left(err)   => evaluated(err)
      }
    }

  def downloadAnalysisResult(mid: Int, fileId: String) = {
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      downloadFile(fileId)
    }
  }

}
