package controllers

import java.nio.file

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document._
import net.scalytica.symbiotic.api.types.{FileId, FolderId, Path}
import no.uio.musit.MusitResults._
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
import scala.util.Try

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
      module: ModuleConstraint,
      modulePath: Path
  )(implicit request: MusitRequest[MultipartFormData[Files.TemporaryFile]]) = {
    if (request.user.canAccess(mid, module, None)) {
      implicit val ctx = ArchiveAddContext(request.user, mid, None)

      request.body.files.headOption.map { tmp =>
        ArchiveDocument(
          title = tmp.filename,
          fileType = tmp.contentType,
          fileSize = Try(file.Files.size(tmp.ref.path)).toOption.map(_.toString),
          stream = Option(FileIO.fromPath(tmp.ref.path))
        )
      }.map { ad =>
        val destPath = modulePath.append(dataIdentifier)
        val res = for {
          _ <- MusitResultT(createIfNotExists(destPath))
          _ <- MusitResultT(
                docService.failIfArchiveDocumentExists(ad.title, destPath)(ctx)
              )
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
          val headers = Seq.newBuilder[(String, String)]
          headers += CONTENT_DISPOSITION -> ContentDisposition(file.title)
          file.size.foreach(s => headers += CONTENT_LENGTH -> s)

          Ok.chunked(source).withHeaders(headers.result(): _*)
        }.getOrElse {
          NotFound(Json.obj("message" -> s"Could not find physical file for $fid"))
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Attachments for Analysis
  // ---------------------------------------------------------------------------

  def uploadAnalysisResult(mid: Int, analysisId: String) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.multipartFormData) {
      implicit request =>
        uploadFile(
          mid = mid,
          dataIdentifier = analysisId,
          module = CollectionManagement,
          modulePath = BaseFolders.AnalysisFolder.path(mid)
        )
    }

  // ---------------------------------------------------------------------------
  // Attachments for CollectionManagement
  // ---------------------------------------------------------------------------

  def getFilesForCollectionManagement(mid: Int, fileIds: String) =
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      parseFileIdsParam(fileIds) match {
        case Right(fids) => metadataForFileIds(fids)
        case Left(err)   => evaluated(err)
      }
    }

  def downloadCollectionManagementFile(mid: Int, fileId: String) = {
    MusitSecureAction(mid, CollectionManagement, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      downloadFile(fileId)
    }
  }

  // ---------------------------------------------------------------------------
  // Attachments for Conservation
  // ---------------------------------------------------------------------------

  def uploadConservationDocument(mid: Int, eventId: String) =
    MusitSecureAction(mid, CollectionManagement, Write).async(parse.multipartFormData) {
      implicit request =>
        uploadFile(
          mid = mid,
          dataIdentifier = eventId,
          module = CollectionManagement,
          modulePath = BaseFolders.ConservationFolder.path(mid)
        )
    }

}
