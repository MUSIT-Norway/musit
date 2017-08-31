package controllers

import com.google.inject.{Inject, Singleton}
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
      docService.getRootFor(mid, includeFiles).map {
        case MusitSuccess(tree)     => ???
        case MusitGeneralError(msg) => ???
        case err: MusitError        => ???
      }
    }

  def addFolder(mid: Int, destFolderId: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      ???
    }

  def updateFolder(mid: Int, folderId: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      ???
    }

  def renameFolder(mid: Int, folderId: String, name: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def getDirectDescendantsById(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def isLockedFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def lockFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def unlockFolder(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def moveFolderTo(mid: Int, folderId: String, to: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def uploadToFolder(mid: Int, folderId: String) =
    MusitSecureAction().async(parse.multipartFormData) { implicit request =>
      ???
    }

  def getPathsFrom(mid: Int, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def getFolderTreeFrom(mid: Int, folderId: String, includeFiles: Boolean) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  // ---------------------------------------------------------------------------
  // File specific endpoints
  // ---------------------------------------------------------------------------

  def uploadToPath(mid: Int, path: String) =
    MusitSecureAction().async(parse.multipartFormData) { implicit request =>
      ???
    }

  def getFileMetadataById(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      docService.getFile(mid, fileId).map {
        case MusitSuccess(maybeDoc) => ??? // return metadata as json
        case err: MusitError        => ???
      }
    }

  def downloadFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      docService.getFile(mid, fileId).map {
        case MusitSuccess(maybeDoc) => ??? // stream back file
        case err: MusitError        => ???
      }
    }

  def isLockedFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def lockFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def unlockFile(mid: Int, fileId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

  def moveFileTo(mid: Int, fileId: String, folderId: String) =
    MusitSecureAction().async { implicit request =>
      ???
    }

}
