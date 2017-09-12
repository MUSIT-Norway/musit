package services

import com.google.inject.{Inject, Singleton}
import models.document.ArchiveTypes.Implicits._
import models.document.ArchiveTypes._
import models.document.Archiveables.{ArchiveFolderItem, ArchiveItem}
import models.document.{ArchiveAddContext, ArchiveContext}
import net.scalytica.symbiotic.api.types._
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

  // TODO: Ensure the relevant root nodes for each museum are created/exist on init.

  def initRootFor(
      mid: MuseumId
  )(implicit ac: ArchiveAddContext): Future[MusitResult[Option[FolderId]]] = {
    dmService.createRootFolder.map(MusitSuccess.apply)
  }

  def rootFolder(
      mid: MuseumId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[ArchiveRoot]]] = {
    dmService.folder(Path.root).map(mr => MusitSuccess(mr.map(f => f: ArchiveRoot)))
  }

  def getRootTreeFor(
      mid: MuseumId,
      includeFiles: Boolean
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[ArchiveItem]]] = {
    val ftree =
      if (includeFiles) dmService.treeWithFiles(Some(Path.root))
      else dmService.treeNoFiles(Some(Path.root))

    ftree.map(tree => MusitSuccess(tree))
  }

  def addFolder(
      mid: MuseumId,
      dest: FolderId,
      afi: ArchiveFolderItem
  )(implicit ac: ArchiveAddContext): Future[MusitResult[Option[FolderId]]] = {
    dmService.folder(dest).flatMap { mdf =>
      mdf.map { df =>
        val p = df.flattenPath.append(afi.title)
        dmService.folderExists(p).flatMap { exists =>
          if (!exists) {
            if (df.isValidParentFor(afi)) {
              // Ensure that the owner and created stamps are set to the
              // values specified in the current context. Also ensure that the
              // Path is correctly set.
              val enriched = afi.enrich().updatePath(p)
              dmService.createFolder(enriched).map(MusitSuccess.apply)
            } else {
              generalErrorF(
                s"${df.flattenPath} is an invalid location for ${afi.getClass}"
              )
            }
          } else {
            generalErrorF(s"Folder ${afi.title} already exists.")
          }
        }
      }.getOrElse {
        generalErrorF(
          "Cannot add folder to destination because " +
            "1) it doesn't exist " +
            "2) insufficient privileges."
        )
      }
    }
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
    dmService.folder(folderId).flatMap { maybeFolder =>
      maybeFolder.map { f =>
        dmService.folder(dest).flatMap { maybeDest =>
          maybeDest.map { d =>
            if (d.isValidParentFor(f)) {
              dmService.moveFolder(f.flattenPath, d.flattenPath.append(f.title)).map {
                case Nil => MusitGeneralError(s"Folder $folderId was not moved.")
                case upd => MusitSuccess(upd)
              }
            } else {
              generalErrorF(s"Moving an ${f.getClass} to an ${d.getClass} isn't allowed")
            }
          }.getOrElse {
            generalErrorF(
              s"Cannot move folder $folderId to $dest because it doesn't exist"
            )
          }
        }
      }.getOrElse {
        generalErrorF(s"Cannot move folder $folderId because it doesn't exist")
      }
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

  def closeFolder(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[Lock]]] = {
    dmService.lockFolder(folderId).map(MusitSuccess.apply)
  }

  def reopenFolder(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFolder(folderId).map(MusitSuccess.apply)
  }

  def getPathsFrom(
      mid: MuseumId,
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[(FileId, Path)]]] = {
    val fmPaths = for {
      f <- OptionT(dmService.folder(folderId))
      p <- OptionT(dmService.treePaths(f.path).map {
            case Nil     => None
            case entries => Some(entries)
          })
    } yield p

    fmPaths.value.map(paths => MusitSuccess(paths.getOrElse(Seq.empty)))
  }

  def getTreeFrom(
      mid: MuseumId,
      folderId: FolderId,
      includeFiles: Boolean
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[ArchiveItem]]] = {
    dmService
      .folder(folderId)
      .flatMap {
        case Some(f) =>
          if (includeFiles) dmService.treeWithFiles(f.path)
          else dmService.treeNoFiles(f.path)

        case None =>
          Future.successful(Seq.empty)

      }
      .map(mfs => MusitSuccess(mfs: Seq[ArchiveItem]))
  }

  // ===========================================================================
  //  Service definitions for interacting with ArchiveDocumentItem data types.
  // ===========================================================================

  def getFile(
      mid: MuseumId,
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[ArchiveDocument]]] = {
    dmService.file(fileId).map(mf => MusitSuccess(mf))
  }

  def isFileLocked(
      mid: MuseumId,
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.fileHasLock(fileId).map(MusitSuccess.apply)
  }

  def lockFile(
      mid: MuseumId,
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[Lock]]] = {
    dmService.lockFile(fileId).map(MusitSuccess.apply)
  }

  def unlockFile(
      mid: MuseumId,
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFile(fileId).map(MusitSuccess.apply)
  }

  def moveFile(
      mid: MuseumId,
      fileId: FileId,
      dest: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Option[ArchiveDocument]]] = {
    val fmPath = dmService.file(fileId).map(_.flatMap(_.path))
    val fmDest = dmService.folder(dest)

    val res = for {
      p <- OptionT(fmPath)
      d <- OptionT(fmDest)
      m <- OptionT(
            dmService
              .moveFile(fileId, p, d.flattenPath)
              .map(mf => mf: Option[ArchiveDocument])
          )
    } yield m

    res.value.map(MusitSuccess.apply)
  }

  // TODO: Add service methods for file upload

}
