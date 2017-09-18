package services

import com.google.inject.{Inject, Singleton}
import controllers.{generalErrorF, notFoundF}
import models.document.Implicits._
import models.document._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.core.DocManagementService
import no.uio.musit.MusitResults.{
  MusitGeneralError,
  MusitNotFound,
  MusitResult,
  MusitSuccess
}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.OptionT
import no.uio.musit.models.MuseumId
import play.api.Logger

import scala.concurrent.Future.{successful => evaluated}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocumentArchiveService @Inject()(
    implicit
    val ec: ExecutionContext,
    val dmService: DocManagementService
) {

  private val log = Logger(getClass)

  // ===========================================================================
  //  Service definitions for interacting with ArchiveFolderItem data types.
  // ===========================================================================

  def archiveRoot(
      mid: MuseumId
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveRoot]] = {
    dmService.folder(Path.root).map {
      case Some(root) => MusitSuccess(root: ArchiveRoot)
      case None       => MusitNotFound(s"Couldn't find root for $mid")
    }
  }

  def getArchiveRootTreeFor(
      includeFiles: Boolean
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[ArchiveItem]]] = {
    val ftree =
      if (includeFiles) dmService.treeWithFiles(Some(Path.root))
      else dmService.treeNoFiles(Some(Path.root))

    ftree.map(tree => MusitSuccess(tree))
  }

  def addArchiveFolderItem(
      dest: FolderId,
      afi: ArchiveFolderItem
  )(implicit ac: ArchiveAddContext): Future[MusitResult[ArchiveFolderItem]] = {
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
              dmService.createFolder(enriched).flatMap {
                case Some(fid) =>
                  getArchiveFolderItem(fid)(ac)

                case None =>
                  generalErrorF(s"ArchiveItemFolder ${afi.title} was not created")
              }
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

  def updateArchiveFolderItem(
      folderId: FolderId,
      afi: ArchiveFolderItem
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveFolderItem]] = {
    dmService.folder(folderId).flatMap {
      case None =>
        log.debug(s"Nothing was updated because folder $folderId doesn't exist.")
        notFoundF(s"Could not find $folderId")

      case Some(existing) =>
        val upd = afi match {
          case a: Archive       => a.copy(path = existing.path, title = existing.title)
          case a: ArchivePart   => a.copy(path = existing.path, title = existing.title)
          case a: ArchiveFolder => a.copy(path = existing.path, title = existing.title)
        }

        dmService.updateFolder(upd).flatMap {
          case Some(_) => getArchiveFolderItem(folderId)
          case None    => generalErrorF(s"Folder $folderId could not be updated.")
        }
    }
  }

  def moveArchiveFolderItem(
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
            notFoundF(s"Cannot move folder $folderId to $dest because it doesn't exist")
          }
        }
      }.getOrElse {
        notFoundF(s"Cannot move folder $folderId because it doesn't exist")
      }
    }
  }

  def renameArchiveFolderItem(
      folderId: FolderId,
      newName: String
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[Path]]] = {
    dmService.folder(folderId).flatMap {
      case None =>
        notFoundF(s"No such folder $folderId")

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
              .map { res =>
                if (res.nonEmpty) MusitSuccess(res)
                else MusitGeneralError(s"Couldn't change name of $folderId to $newName")
              }
          }
        }
    }
  }

  def getArchiveFolderItem(
      folderId: FolderId
  )(
      implicit ac: ArchiveContext
  ): Future[MusitResult[ArchiveFolderItem]] = {
    dmService.folder(folderId).map {
      case Some(afi) => MusitSuccess(afi)
      case None      => MusitNotFound(s"Couldn't find folder $folderId")
    }
  }

  def isArchiveFolderItemClosed(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.folderHasLock(folderId).map(MusitSuccess.apply)
  }

  def closeArchiveFolderItem(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Lock]] = {
    dmService.lockFolder(folderId).map {
      case Some(lock) => MusitSuccess(lock)
      case None       => MusitGeneralError(s"Lock was not applied to $folderId")
    }
  }

  def openArchiveFolderItem(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFolder(folderId).map(MusitSuccess.apply)
  }

  def getPathsFrom(
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
          evaluated(Seq.empty)

      }
      .map(mfs => MusitSuccess(mfs: Seq[ArchiveItem]))
  }

  def getChildrenFor(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[ArchiveItem]]] = {
    dmService.folder(folderId).flatMap { maybeFolder =>
      maybeFolder.map { f =>
        dmService
          .childrenWithFiles(f.metadata.path)
          .map(_.map(mf => mf: ArchiveItem))
          .map(MusitSuccess.apply)
      }.getOrElse {
        generalErrorF(s"Could not find folder $folderId")
      }
    }
  }

  // ===========================================================================
  //  Service definitions for interacting with ArchiveDocumentItem data types.
  // ===========================================================================

  def saveArchiveDocument(
      dest: FolderId,
      ad: ArchiveDocument
  )(implicit ac: ArchiveAddContext): Future[MusitResult[FileId]] = {
    dmService.folder(dest).flatMap { maybeDest =>
      maybeDest.map { df =>
        val enriched = ad.enrich().updatePath(df.flattenPath)
        dmService.saveFile(enriched).map {
          case Some(fid) => MusitSuccess(fid)
          case None      => MusitGeneralError(s"File ${ad.title} was not saved.")
        }
      }.getOrElse {
        notFoundF(s"Unable to save ArchiveDocument in $dest because it doesn't exist")
      }
    }
  }

  def updateArchiveDocument(
      fileId: FileId,
      ad: ArchiveDocument
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveDocument]] = {
    dmService.file(fileId).flatMap { maybeFile =>
      maybeFile.map { _ =>
        dmService.updateFile(ad.copy(fid = Some(fileId))).flatMap {
          case Some(_) => getArchiveDocument(fileId)
          case None    => generalErrorF(s"File $fileId could not be updated.")
        }
      }.getOrElse {
        notFoundF(
          s"Unable to update ArchiveDocument $fileId because it doesn't exist"
        )
      }
    }
  }

  def getArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveDocument]] = {
    dmService.file(fileId).map {
      case Some(ad) => MusitSuccess(ad)
      case None     => MusitNotFound(s"Could not find ArchiveDocument $fileId")
    }
  }

  def isArchiveDocumentLocked(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.fileHasLock(fileId).map(MusitSuccess.apply)
  }

  def lockArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Lock]] = {
    dmService.lockFile(fileId).map {
      case Some(lock) => MusitSuccess(lock)
      case None       => MusitGeneralError(s"Lock was not applied to $fileId")
    }
  }

  def unlockArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFile(fileId).map(MusitSuccess.apply)
  }

  def moveArchiveDocument(
      fileId: FileId,
      dest: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveDocument]] = {
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

    res.value.map {
      case Some(ad) => MusitSuccess(ad)
      case None     => MusitGeneralError(s"ArchiveDocument $fileId was not moved.")
    }
  }

}
