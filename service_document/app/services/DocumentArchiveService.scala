package services

import com.google.inject.{Inject, Singleton}
import models.document.ArchiveContext
import models.document.ArchiveFolders.Implicits._
import models.document.ArchiveFolders.{Archive, ArchiveFolder, ArchivePart}
import models.document.ArchiveItems.ArchiveFolderItem
import net.scalytica.symbiotic.api.types.{FolderId, Lock, Path}
import net.scalytica.symbiotic.core.DocManagementService
import no.uio.musit.MusitResults.{MusitGeneralError, MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.OptionT
import no.uio.musit.models.MuseumId
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocumentArchiveService @Inject()(
    implicit
    val ec: ExecutionContext,
    val dmService: DocManagementService
) {

  private val logger = Logger(classOf[DocumentArchiveService])

  private def generalErrorF(msg: String) = Future.successful(MusitGeneralError(msg))

  // ===========================================================================
  //  Service definitions for interacting with ArchiveFolderItem data types.
  // ===========================================================================
  def initRootFor(
      mid: MuseumId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[FolderId]]] = {
    dmService.createRootFolder.map(MusitSuccess.apply)
  }

  def addFolder(
      mid: MuseumId,
      dest: FolderId,
      afi: ArchiveFolderItem
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[FolderId]]] = {
    afi.path.map { p =>
      dmService.folderExists(p).flatMap { exists =>
        if (!exists) {
          dmService.folder(dest).flatMap { mdf =>
            mdf.map { df =>
              if (df.isValidParentFor(afi)) {
                dmService.createFolder(afi).map(MusitSuccess.apply)
              } else {
                generalErrorF(
                  s"${df.flattenPath} is an invalid location for ${afi.getClass}"
                )
              }
            }.getOrElse {
              generalErrorF("Cannot add folder to destination because it doesn't exist.")
            }
          }
        } else {
          generalErrorF(s"Folder ${afi.title} already exists.")
        }
      }
    }.getOrElse(generalErrorF(s"Folder does not contain a valid destination path."))
  }

  def updateFolder(
      mid: MuseumId,
      folderId: FolderId,
      afi: ArchiveFolderItem
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[FolderId]]] = {
    dmService.folder(folderId).flatMap {
      case None =>
        logger.debug(s"Nothing was updated because folder $folderId doesn't exist.")
        Future.successful(MusitSuccess(None))

      case Some(existing) =>
        val upd = afi match {
          case a: Archive        => a.copy(path = existing.path, title = existing.title)
          case ap: ArchivePart   => ap.copy(path = existing.path, title = existing.title)
          case af: ArchiveFolder => af.copy(path = existing.path, title = existing.title)
        }

        dmService.updateFolder(upd).map {
          case None =>
            logger.debug(s"Folder $folderId was not updated")
            MusitSuccess(None)

          case ok =>
            MusitSuccess(ok)
        }
    }
  }

  def moveFolder(
      mid: MuseumId,
      folderId: FolderId,
      dest: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[Path]]] = {
    val fmf = dmService.folder(folderId)
    val fmd = dmService.folder(dest)

    val res = for {
      f <- OptionT(fmf)
      d <- OptionT(fmd)
      m <- OptionT(
            dmService.moveFolder(f.flattenPath, d.flattenPath.append(f.title)).map {
              case Nil => None
              case upd => Some(upd)
            }
          )
    } yield m

    res.value.map {
      case Some(upd) => MusitSuccess(upd)
      case None      => MusitGeneralError(s"Folder $folderId was not moved.")
    }
  }

  def renameFolder(
      mid: MuseumId,
      folderId: FolderId,
      newName: String
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[Path]]] = {
    dmService.folder(folderId).flatMap {
      case None =>
        generalErrorF(s"No such folder $folderId")

      case Some(f) =>
        dmService.treeNoFiles(f.path.map(_.parent)).flatMap { siblings =>
          if (siblings.exists(f => f.filename == newName)) {
            generalErrorF(
              s"There is already a folder called $newName at ${f.flattenPath.parent}"
            )
          } else {
            // Rename the folder by moving it. Works similarly as the mv cmd in linux.
            dmService
              .moveFolder(f.flattenPath, f.flattenPath.parent.append(newName))
              .map(MusitSuccess.apply)
          }
        }
    }
  }

  def getFolder(
      mid: MuseumId,
      folderId: FolderId
  )(
      implicit ac: ArchiveContext
  ): Future[MusitResult[Option[ArchiveFolderItem]]] = {
    dmService.folder(folderId).map(mf => MusitSuccess(mf))
  }

  def isFolderLocked(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.folderHasLock(folderId).map(MusitSuccess.apply)
  }

  def lockFolder(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[Lock]]] = {
    dmService.lockFolder(folderId).map(MusitSuccess.apply)
  }

  def unlockFolder(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFolder(folderId).map(MusitSuccess.apply)
  }

  // ===========================================================================
  //  Service definitions for interacting with ArchiveDocumentItem data types.
  // ===========================================================================


  // ===========================================================================
  //  Service definitions for interacting with the virtual filesystem tree
  // ===========================================================================
}
