package controllers

import com.google.inject.{Inject, Singleton}
import net.scalytica.symbiotic.api.types.FolderId
import no.uio.musit.security.{Authenticator, DocumentArchive}
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

  def addFolder(mid: Int, destFolderId: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      ???
    }

  def updateFolder(mid: Int, folderId: String) =
    MusitSecureAction().async(parse.json) { implicit request =>
      // TODO: Ensure that the folder PATH is not updatable through this endpoint!!!
      ???
    }

  def getDirectDescendantsById(mid: Int, folderId: String) =
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

  def isLockedFolder(mid: Int, folderId: String) =
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

  // ---------------------------------------------------------------------------
  // File specific endpoints
  // ---------------------------------------------------------------------------

  /*
    File endpoints:

      POST        /museum/:mid/files/upload              uploadWithPath(mid: Int, path: String)
      GET         /museum/:mid/files/:fileId             getFileById(mid: Int, fileId: String)
      PUT         /museum/:mid/files/:fileId/lock        lockFile(mid: Int, fileId: String)
      PUT         /museum/:mid/files/:fileId/unlock      unlockFile(mid: Int, fileId: String)
      GET         /museum/:mid/files/:fileId/islocked    isLockedFile(mid: Int, fileId: String)
      PUT         /museum/:mid/files/:fileId/move        moveFileTo(mid: Int, fileId: String, to: String)
   */

  // ---------------------------------------------------------------------------
  // FSTree specific endpoints
  // ---------------------------------------------------------------------------

  /*
    Tree endpoints:

      GET         /museum/:mid/tree                      getRootTree(mid: Int, includeFiles: Boolean ?= false)
      GET         /museum/:mid/tree/paths                getTreePaths(mid: Int, path: Option[String])
      GET         /museum/:mid/tree/hierarchy            getFolderHierarchy(mid: Int, path: Option[String])
      GET         /museum/:mid/subtree                   getSubTree(mid: Int, path: String, includeFiles: Boolean ?= false)

 */

}
