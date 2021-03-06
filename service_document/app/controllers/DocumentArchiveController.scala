package controllers

import java.nio.file.Files

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document.{ArchiveAddContext, ArchiveContext, _}
import net.scalytica.symbiotic.json.Implicits.{PathFormatters, lockFormat}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.security.Permissions.{Admin, Read, Write}
import no.uio.musit.security.{Authenticator, DocumentArchive}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.ControllerComponents
import services.DocumentArchiveService

import scala.concurrent.Future.{successful => evaluated}
import scala.util.Try

@Singleton
class DocumentArchiveController @Inject()(
    val controllerComponents: ControllerComponents,
    val authService: Authenticator,
    val docService: DocumentArchiveService
) extends MusitController {

  private val log = Logger(classOf[DocumentArchiveController])

  // ---------------------------------------------------------------------------
  // Folder specific endpoints
  // ---------------------------------------------------------------------------

  def getRootTree(mid: Int, includeFiles: Boolean) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getArchiveRootTreeFor(includeFiles).map { r =>
        respond(r) { tree =>
          if (tree.isEmpty) NoContent
          else Ok(Json.toJson[Seq[ArchiveItem]](tree))
        }
      }
    }

  def addFolder(mid: Int, destFolderId: String, collectionId: Option[String]) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.json) { implicit request =>
      // Verify that the user has access to collectionId
      parseMaybeCollectionIdParam(collectionId) match {
        case Right(maybeColId) =>
          if (request.user.canAccess(mid, DocumentArchive, maybeColId)) {
            implicit val ctx = ArchiveAddContext(request.user, mid, maybeColId)

            request.body.validate[ArchiveFolderItem] match {
              case JsSuccess(afi, _) =>
                docService.addArchiveFolderItem(destFolderId, afi).map { r =>
                  respond(r)(added => Created(Json.toJson[ArchiveFolderItem](added)))
                }

              case err: JsError =>
                evaluated(BadRequest(JsError.toJson(err)))
            }
          } else {
            evaluated(Forbidden(Json.obj("message" -> s"Unauthorized access")))
          }

        case Left(err) => evaluated(err)
      }

    }

  def getFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getArchiveFolderItem(folderId).map { r =>
        respond(r) { afi =>
          Ok(Json.toJson[ArchiveFolderItem](afi))
        }
      }
    }

  def updateFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.json) { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      request.body.validate[ArchiveFolderItem] match {
        case JsSuccess(afi, _) =>
          docService.updateArchiveFolderItem(folderId, afi).map { r =>
            respond(r)(d => Ok(Json.toJson[ArchiveFolderItem](d)))
          }

        case err: JsError =>
          evaluated(BadRequest(JsError.toJson(err)))
      }
    }

  def renameFolder(mid: Int, folderId: String, name: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.renameArchiveFolderItem(folderId, name).map { r =>
        respond(r) { modPaths =>
          if (modPaths.nonEmpty) Ok(Json.toJson(modPaths))
          else NotModified
        }
      }
    }

  def getDirectChildrenForId(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getChildrenFor(folderId).map { r =>
        respond(r) { tree =>
          if (tree.nonEmpty) Ok(Json.toJson[Seq[ArchiveItem]](tree))
          else NoContent
        }
      }
    }

  def isClosedFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.isArchiveFolderItemClosed(folderId).map { r =>
        respond(r) { locked =>
          Ok(Json.obj("isLocked" -> locked))
        }
      }
    }

  def closeFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Admin).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.closeArchiveFolderItem(folderId).map { r =>
        respond(r)(l => Ok(Json.toJson(l)))
      }
    }

  def openFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Admin).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.openArchiveFolderItem(folderId).map { r =>
        respond(r)(opened => if (opened) Ok else NotModified)
      }
    }

  def moveFolderTo(mid: Int, folderId: String, to: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.moveArchiveFolderItem(folderId, to).map { r =>
        respond(r) { modPaths =>
          if (modPaths.nonEmpty) Ok(Json.toJson(modPaths))
          else NotModified
        }
      }
    }

  def getFolderTreeFrom(mid: Int, folderId: String, includeFiles: Boolean) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getTreeFrom(folderId, includeFiles).map { r =>
        respond(r) { tree =>
          if (tree.nonEmpty) Ok(Json.toJson[Seq[ArchiveItem]](tree))
          else NoContent
        }
      }
    }

  // ---------------------------------------------------------------------------
  // File specific endpoints
  // ---------------------------------------------------------------------------

  def uploadToFolder(mid: Int, folderId: String, collectionId: Option[String]) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.multipartFormData) {
      implicit request =>
        // Verify that the user has access to collectionId
        parseMaybeCollectionIdParam(collectionId) match {
          case Right(maybeColId) =>
            if (request.user.canAccess(mid, DocumentArchive, maybeColId)) {
              implicit val ctx = ArchiveAddContext(request.user, mid, maybeColId)

              request.body.files.headOption.map { tmp =>
                ArchiveDocument(
                  title = tmp.filename,
                  fileType = tmp.contentType,
                  fileSize = Try(Files.size(tmp.ref.path)).toOption.map(_.toString),
                  stream = Option(FileIO.fromPath(tmp.ref.path))
                )
              }.map { ad =>
                val res = for {
                  a <- MusitResultT(docService.saveArchiveDocument(folderId, ad))
                  b <- MusitResultT(docService.getArchiveDocument(a)(ctx))
                } yield b

                res.value.map {
                  case MusitSuccess(added) =>
                    // Using the less specific type ArchiveItem for JSON parsing
                    Created(Json.toJson[ArchiveItem](added))

                  case MusitGeneralError(msg) =>
                    BadRequest(Json.obj("message" -> msg))

                  case err: MusitError =>
                    InternalServerError(Json.obj("message" -> s"${err.message}"))
                }
              }.getOrElse(
                evaluated(BadRequest(Json.obj("message" -> s"No attached file")))
              )

            } else {
              evaluated(Forbidden(Json.obj("message" -> s"Unauthorized access")))
            }

          case Left(err) => evaluated(err)
        }
    }

  def updateFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.json) { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      request.body.validate[ArchiveDocument] match {
        case JsSuccess(ad, _) =>
          docService.updateArchiveDocument(fileId, ad).map { r =>
            respond(r)(d => Ok(Json.toJson[ArchiveItem](d)))
          }

        case err: JsError =>
          evaluated(BadRequest(JsError.toJson(err)))
      }
    }

  def getFileMetadataById(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map { r =>
        respond(r)(d => Ok(Json.toJson[ArchiveItem](d)))
      }
    }

  def downloadFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map { r =>
        respond(r) { doc =>
          doc.stream.map { source =>
            val headers = Seq.newBuilder[(String, String)]
            headers += CONTENT_DISPOSITION -> ContentDisposition(doc.title)
            doc.size.foreach(s => headers += CONTENT_LENGTH -> s)

            Ok.chunked(source).withHeaders(headers.result(): _*)
          }.getOrElse {
            NotFound(Json.obj("message" -> s"Could not find physical file for $fileId"))
          }
        }
      }
    }

  def isLockedFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.isArchiveDocumentLocked(fileId).map { r =>
        respond(r)(l => Ok(Json.obj("isLocked" -> l)))
      }
    }

  def lockFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.lockArchiveDocument(fileId).map { r =>
        respond(r)(l => Ok(Json.toJson(l)))
      }
    }

  def unlockFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.unlockArchiveDocument(fileId).map { r =>
        respond(r)(opened => if (opened) Ok else NotModified)
      }
    }

  def moveFileTo(mid: Int, fileId: String, to: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.moveArchiveDocument(fileId, to).map { r =>
        respond(r)(d => Ok(Json.toJson[ArchiveItem](d)))
      }
    }

}
