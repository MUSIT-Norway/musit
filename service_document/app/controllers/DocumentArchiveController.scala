package controllers

import com.google.inject.{Inject, Singleton}
import models.document.ArchiveContext
import no.uio.musit.MusitResults.{MusitError, MusitGeneralError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.ControllerComponents
import services.DocumentArchiveService

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
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)

      docService.getRootTreeFor(mid, includeFiles).map {
        case MusitSuccess(tree)     => ???
        case MusitGeneralError(msg) => ???
        case err: MusitError        => ???
      }
    }

  def addFolder(mid: Int, destFolderId: String, collectionId: Option[String]) =
    MusitSecureAction().async(parse.json) { implicit request =>
      // TODO: Verify that the user has access to collectionId
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def updateFolder(mid: Int, folderId: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def renameFolder(mid: Int, folderId: String, name: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def getDirectDescendantsById(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def isLockedFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def lockFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def unlockFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def moveFolderTo(mid: Int, folderId: String, to: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def getPathsFrom(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def getFolderTreeFrom(mid: Int, folderId: String, includeFiles: Boolean) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  // ---------------------------------------------------------------------------
  // File specific endpoints
  // ---------------------------------------------------------------------------

  def uploadToFolder(mid: Int, folderId: String, collectionId: Option[String]) =
    MusitSecureAction().async(parse.multipartFormData) { implicit request =>
      // TODO: Verify that the user has access to collectionId
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def uploadToPath(mid: Int, path: String, collectionId: Option[String]) =
    MusitSecureAction().async(parse.multipartFormData) { implicit request =>
      // TODO: Verify that the user has access to collectionId
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def getFileMetadataById(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getFile(mid, fileId).map {
        case MusitSuccess(maybeDoc) => ??? // return metadata as json
        case err: MusitError        => ???
      }
    }

  def downloadFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      docService.getFile(mid, fileId).map {
        case MusitSuccess(maybeDoc) => ??? // stream back file
        case err: MusitError        => ???
      }
    }

  def isLockedFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def lockFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def unlockFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

  def moveFileTo(mid: Int, fileId: String, folderId: String) =
    MusitSecureAction().async { implicit request =>
      implicit val ctx = ArchiveContext(request.user, mid)
      ???
    }

}
