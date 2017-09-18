package controllers

import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import models.document.{ArchiveAddContext, ArchiveContext, _}
import net.scalytica.symbiotic.json.Implicits.{PathFormatters, lockFormat}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.security.Permissions.{Read, Write}
import no.uio.musit.security.{Authenticator, DocumentArchive}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.ControllerComponents
import services.DocumentArchiveService

import scala.concurrent.Future.{successful => evaluated}

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
      val colId = collectionId.map(id => Collection.fromString(id).uuid)
      if (request.user.canAccess(mid, DocumentArchive, colId)) {
        implicit val ctx = ArchiveAddContext(request.user, mid, colId)

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

      docService.isArchiveDocumentLocked(folderId).map { r =>
        respond(r) { locked =>
          Ok(Json.obj("isLocked" -> locked))
        }
      }
    }

  def closeFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.closeArchiveFolderItem(folderId).map { r =>
        respond(r)(l => Ok(Json.toJson(l)))
      }
    }

  def openFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
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
        val colId = collectionId.map(id => Collection.fromString(id).uuid)
        if (request.user.canAccess(mid, DocumentArchive, colId)) {
          implicit val ctx = ArchiveAddContext(request.user, mid, colId)

          request.body.files.headOption.map { tmp =>
            ArchiveDocument(
              title = tmp.filename,
              fileType = tmp.contentType,
              stream = Option(FileIO.fromPath(tmp.ref.path))
            )
          }.map { ad =>
            val res = for {
              a <- MusitResultT(docService.saveArchiveDocument(folderId, ad))
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

  def updateFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.json) { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      request.body.validate[ArchiveDocument] match {
        case JsSuccess(ad, _) =>
          docService.updateArchiveDocument(fileId, ad).map { r =>
            respond(r)(d => Ok(Json.toJson[ArchiveDocumentItem](d)))
          }

        case err: JsError =>
          evaluated(BadRequest(JsError.toJson(err)))
      }
    }

  def getFileMetadataById(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map { r =>
        respond(r)(d => Ok(Json.toJson[ArchiveDocumentItem](d)))
      }
    }

  def downloadFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map { r =>
        respond(r) { doc =>
          doc.stream.map { source =>
            Ok.chunked(source)
              .withHeaders(CONTENT_DISPOSITION -> ContentDisposition(doc.title))
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
        respond(r)(d => Ok(Json.toJson(d)))
      }
    }

}
