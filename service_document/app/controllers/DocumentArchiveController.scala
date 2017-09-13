package controllers

import java.net.URLEncoder.encode

import com.google.inject.{Inject, Singleton}
import models.document.{ArchiveAddContext, ArchiveContext}
import models.document.ArchiveTypes._
import net.scalytica.symbiotic.json.Implicits.{PathFormatters, lockFormat}
import no.uio.musit.MusitResults.{MusitError, MusitGeneralError, MusitSuccess}
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

  private val logger = Logger(classOf[DocumentArchiveController])

  // ---------------------------------------------------------------------------
  // Folder specific endpoints
  // ---------------------------------------------------------------------------

  def getRootTree(mid: Int, includeFiles: Boolean) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getArchiveRootTreeFor(includeFiles).map {
        case MusitSuccess(tree) =>
          if (tree.isEmpty) NoContent
          else Ok(Json.toJson[Seq[ArchiveItem]](tree))

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
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
            docService.addArchiveFolderItem(destFolderId, afi).map {
              case MusitSuccess(Some(added)) =>
                Created(Json.toJson[ArchiveFolderItem](added))

              case MusitSuccess(None) =>
                InternalServerError(
                  Json.obj("msg" -> "Could not find the folder that was added")
                )

              case MusitGeneralError(msg) =>
                BadRequest(Json.obj("msg" -> msg))

              case err: MusitError =>
                InternalServerError(Json.obj("msg" -> err.message))
            }

          case err: JsError =>
            evaluated(BadRequest(JsError.toJson(err)))
        }
      } else {
        evaluated(Forbidden(Json.obj("msg" -> s"Unauthorized access")))
      }
    }

  def updateFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async(parse.json) { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      // TODO: implement me

      ???
    }

  def renameFolder(mid: Int, folderId: String, name: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.renameArchiveFolderItem(folderId, name).map {
        case MusitSuccess(modPaths) =>
          if (modPaths.nonEmpty) Ok(Json.toJson(modPaths))
          else NotModified

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def getDirectChildrenForId(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getChildrenFor(folderId).map {
        case MusitSuccess(tree) =>
          if (tree.nonEmpty) Ok(Json.toJson[Seq[ArchiveItem]](tree))
          else NoContent

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def isClosedFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.isArchiveDocumentLocked(folderId).map {
        case MusitSuccess(res) =>
          Ok(Json.obj("isLocked" -> res))

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def closeFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.closeArchiveFolderItem(folderId).map {
        case MusitSuccess(maybeLock) =>
          maybeLock.map(l => Ok(Json.toJson(l))).getOrElse(NotModified)

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def openFolder(mid: Int, folderId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.openArchiveFolderItem(folderId).map {
        case MusitSuccess(opened) =>
          if (opened) Ok else NotModified

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def moveFolderTo(mid: Int, folderId: String, to: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.moveArchiveFolderItem(folderId, to).map {
        case MusitSuccess(modPaths) =>
          if (modPaths.nonEmpty) Ok(Json.toJson(modPaths))
          else NotModified

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def getFolderTreeFrom(mid: Int, folderId: String, includeFiles: Boolean) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getTreeFrom(folderId, includeFiles).map {
        case MusitSuccess(tree) =>
          if (tree.nonEmpty) Ok(Json.toJson[Seq[ArchiveItem]](tree))
          else NoContent

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
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

          // TODO: implement me
          ???
        } else {
          evaluated(Forbidden(Json.obj("msg" -> s"Unauthorized access")))
        }
    }

  // TODO: Implement endpoint for updating ArchiveDocumentItem metadata

  def getFileMetadataById(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map {
        case MusitSuccess(maybeDoc) =>
          maybeDoc.map(d => Ok(Json.toJson[ArchiveDocumentItem](d))).getOrElse(NotFound)

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def downloadFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getArchiveDocument(fileId).map {
        case MusitSuccess(maybeDoc) =>
          maybeDoc.map { doc =>
            doc.stream.map { source =>
              val cd =
                s"""attachment; filename="${doc.filename}"; filename*=UTF-8''""" +
                  encode(doc.filename, "UTF-8").replace("+", "%20")

              Ok.chunked(source).withHeaders(CONTENT_DISPOSITION -> cd)
            }.getOrElse(NotFound)
          }.getOrElse(NotFound)

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def isLockedFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Read).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.isArchiveDocumentLocked(fileId).map {
        case MusitSuccess(res) =>
          Ok(Json.obj("isLocked" -> res))

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def lockFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.lockArchiveDocument(fileId).map {
        case MusitSuccess(maybeLock) =>
          maybeLock.map(l => Ok(Json.toJson(l))).getOrElse(NotModified)

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def unlockFile(mid: Int, fileId: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.unlockArchiveDocument(fileId).map {
        case MusitSuccess(opened) =>
          if (opened) Ok else NotModified

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

  def moveFileTo(mid: Int, fileId: String, to: String) =
    MusitSecureAction(mid, DocumentArchive, Write).async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.moveArchiveDocument(fileId, to).map {
        case MusitSuccess(modPaths) =>
          if (modPaths.nonEmpty) Ok(Json.toJson(modPaths))
          else NotModified

        case MusitGeneralError(msg) =>
          BadRequest(Json.obj("msg" -> msg))

        case err: MusitError =>
          InternalServerError(Json.obj("msg" -> err.message))
      }
    }

}
