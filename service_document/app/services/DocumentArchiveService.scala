package services

import com.google.inject.{Inject, Singleton}
import models.document.Implicits._
import models.document._
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.core.DocManagementService
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.{MusitResultT, OptionT}
import no.uio.musit.models.MuseumId
import play.api.Logger

import scala.concurrent.Future.{successful => evaluated}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

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

  /**
   * Fetch the ArchiveRoot folder for the provided MuseumId
   *
   * @param mid the MuseumId
   * @param ac  ArchiveContext
   * @return eventually a result containing the ArchiveRoot data
   */
  def archiveRoot(
      mid: MuseumId
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveRoot]] = {
    dmService.folder(Path.root).map {
      case Some(root) => MusitSuccess(root: ArchiveRoot)
      case None       => MusitNotFound(s"Couldn't find root for $mid")
    }
  }

  /**
   * Fetch the tree for the MuseumId set as Owner in the ArchiveContext.
   *
   * @param includeFiles if set to true result will also include
   *                     ArchiveDocumentItems. Otherwise only ArchiveFolderItems
   *                     are returned.
   * @param ac           ArchiveContext
   * @return eventually a result containing a list of ArchiveItems
   */
  def getArchiveRootTreeFor(
      includeFiles: Boolean
  )(implicit ac: ArchiveContext): Future[MusitResult[Seq[ArchiveItem]]] = {
    val ftree =
      if (includeFiles) dmService.treeWithFiles(Some(Path.root))
      else dmService.treeNoFiles(Some(Path.root))

    ftree.map(tree => MusitSuccess(tree))
  }

  /**
   * Method for adding a new ArchiveFolderItem to the provided destination FolderId.
   *
   * @param dest FolderId to add a new ArchiveFolderItem to
   * @param afi  The ArchiveFolder to add
   * @param ac   ArchiveAddContext
   * @return eventually returns a result containing the newly added ArchiveFolderItem.
   */
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

  /**
   * Service method for renaming an ArchiveFolderItem.
   *
   * Under the hood, in the document management library, renaming and move
   * operations are indistinguishable from each other. The operation is modelled
   * after the unix `mv` command.
   *
   * In this case, the difference between move and rename involves validation of
   * rules for folders being moved, that do not apply when renaming.
   *
   * @param folderId the FolderId to rename
   * @param newName  the new name to give the folder
   * @param ac       ArchiveContext
   * @return Eventually a result containing a list of the paths that were
   *         affected by the renaming. Typically _all_ children of the renamed
   *         folder will get their paths updated.
   */
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

  /**
   * Move a folder from its current parent folder to another.
   *
   * @param folderId the FolderId to move
   * @param dest     the FolderId of the new parent.
   * @param ac       ArchiveContext
   * @return Eventually a result containing a list of the paths that were
   *         affected by the move. Typically _all_ children of the moved folder
   *         will get their paths updated.
   */
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

  /**
   * Method for updating the metadata content of an ArchiveFolderItem.
   *
   * The underlying library (symbiotic-postgres) allows updating the following
   * fields:
   *
   * <ul>
   * <li>fileType</li>
   * <li>description</li>
   * <li>customMetadata - See how an ArchiveFolderItem is mapped to underlying model.</li>
   * </ul>
   *
   * @param folderId the FolderId of the folder to update
   * @param afi      the updated data
   * @param ac       ArchiveContext
   * @return eventually returns a result containing the updated ArchiveFolderItem
   */
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

  /**
   * Try to fetch the folder with the given FolderId
   *
   * @param folderId the FolderId to fetch
   * @param ac       ArchiveContext
   * @return eventually a result that contains the ArchiveFolderItem
   */
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

  /**
   * Check if there a folder with the given Path exists
   *
   * @param path the Path to look for
   * @param ac   ArchiveContext
   * @return eventually a result containing a Boolean with value true if folder
   *         exists, otherwise false.
   */
  def archiveFolderItemPathExists(
      path: Path
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.folderExists(path).map(MusitSuccess.apply)
  }

  /**
   * Method that allows creating a GenericFolder at the given path. These types
   * of folders live _outside_ the sub-tree represented by the Archive typed
   * folders. Typically used for building the "Modules" folder structure which
   * is a sibling to each museums Archive.
   *
   * @param path the Path to create the folder at
   * @param ad   ArchiveContext
   * @return eventually a result contains an Option of FolderId if the folder
   *         was created.
   */
  def createGenericFolderItemAtPath(
      path: Path
  )(implicit ad: ArchiveAddContext): Future[MusitResult[Option[FolderId]]] = {
    dmService.createFolder(GenericFolder(path).enrich()).map(MusitSuccess.apply)
  }

  /**
   * Method for checking whether or not an ArchiveFolderItem is closed or not
   *
   * @param folderId the FolderId to check
   * @param ac ArchiveContext
   * @return eventually a result containing a Boolean true if closed, else false
   */
  def isArchiveFolderItemClosed(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.folderHasLock(folderId).map(MusitSuccess.apply)
  }

  /**
   * Will CLOSE an ArchiveFolderItem, which has the effect of not allowing any
   * more changes to the given folder.
   *
   * @param folderId the FolderId to close
   * @param ac ArchiveContext
   * @return eventually a result with a Lock instance if the folder was
   *         successfully closed.
   */
  def closeArchiveFolderItem(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Lock]] = {
    dmService.lockFolder(folderId).map {
      case Some(lock) => MusitSuccess(lock)
      case None       => MusitGeneralError(s"Lock was not applied to $folderId")
    }
  }

  /**
   * Will (re)OPEN a closed ArchiveFolderItem. Making it possible to modify the
   * folder and its sub-tree again.
   *
   * @param folderId the FolderId to re-open
   * @param ac ArchiveContext
   * @return eventually a result with a Boolean true if the re-opening was successfull.
   */
  def openArchiveFolderItem(
      folderId: FolderId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFolder(folderId).map(MusitSuccess.apply)
  }

  /**
   * Handy method for locating all FileIds and their Paths in the sub-tree of a
   * given FolderId.
   *
   * @param folderId the FolderId to get child paths for
   * @param ac ArchiveContext
   * @return eventually a result containing a list of tuples of types FileId and Path
   *         for all the nodes in the sub-tree of the given folderId.
   */
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

  /**
   * Will fetch all ArchiveItem nodes in the sub-tree of the given FolderId.
   *
   * @param folderId the folder Id to get sub-tree nodes for
   * @param includeFiles Boolean flag to say if sub-tree should include files or not
   * @param ac ArchiveContext
   * @return eventually a result containing a list of ArchiveItems in the sub-tree
   */
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

  /**
   * In contrast to the {{{getTreeFrom}}} method, this method ONLY returns the
   * ArchiveItem nodes that are direct children to the specified FolderId.
   *
   * @param folderId the FolderId to get direct children for
   * @param ac ArchiveContext
   * @return eventually a result with a list of direct children of the given folder
   */
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

  /**
   * Saves a new ArchiveDocument in the given destination FolderId. This does
   * not allow for adding ArchiveDocuments that do _not_ contain an actual file.
   *
   * @param dest the FolderId where the file should be saved to
   * @param ad the ArchiveDocument to save
   * @param ac ArchiveAddContext
   * @return eventually a result containing the newly created FileId
   */
  def saveArchiveDocument(
      dest: FolderId,
      ad: ArchiveDocument
  )(implicit ac: ArchiveAddContext): Future[MusitResult[FileId]] = {
    dmService.folder(dest).flatMap { maybeFolder =>
      maybeFolder.map { df =>
        saveArchiveDocument(df, ad)
      }.getOrElse {
        notFoundF(s"Unable to save ArchiveDocument in $dest because it doesn't exist")
      }
    }
  }

  /**
   * Saves a new ArchiveDocument in the given destination Path. This does
   * not allow for adding ArchiveDocuments that do _not_ contain an actual file.
   *
   * @param dest the Path where the file should be saved to
   * @param ad the ArchiveDocument to save
   * @param ac ArchiveContext
   * @return eventually a result containing the newly created FileId
   */
  def saveArchiveDocument(
      dest: Path,
      ad: ArchiveDocument
  )(implicit ac: ArchiveAddContext): Future[MusitResult[FileId]] = {
    dmService.folder(dest).flatMap { maybeFolder =>
      maybeFolder.map { df =>
        saveArchiveDocument(df, ad)
      }.getOrElse {
        notFoundF(s"Unable to save ArchiveDocument in $dest because it doesn't exist")
      }
    }
  }

  /**
   * Saves a new ArchiveDocument in the given ArchiveFolderItem. This does not
   * allow for adding ArchiveDocuments that do _not_ contain an actual file.
   *
   * @param folder the ArchiveFolderItem the file should be saved to
   * @param ad the ArchiveDocument to save
   * @param ac ArchiveContext
   * @return eventually a result containing the newly created FileId
   */
  private def saveArchiveDocument(
      folder: ArchiveFolderItem,
      ad: ArchiveDocument
  )(implicit ac: ArchiveAddContext): Future[MusitResult[FileId]] = {
    val enriched = ad.enrich().updatePath(folder.flattenPath)
    dmService.latestFile(enriched.title, enriched.path).flatMap {
      case None =>
        dmService.saveFile(enriched).map {
          case Some(fid) => MusitSuccess(fid)
          case None      => MusitGeneralError(s"File ${ad.title} was not saved.")
        }

      case Some(exists) =>
        evaluated(
          MusitValidationError(
            s"Can't add file because a file with the name ${ad.title} already" +
              s" exists in ${ad.path}"
          )
        )
    }

  }

  /**
   * Updates the metadata for the given FileId with the content of the values
   * defined in the ArchiveDocument argument. It is possible to modify the
   * following fields in the underlying File type
   *
   * <ul>
   *   <li>description</li>
   *   <li>customMetadata</li>
   * </ul>
   *
   * @param fileId the FileId of the ArchiveDocument to update
   * @param ad ArchiveDocument with updated metadata values
   * @param ac ArchiveContext
   * @return eventually a result containing the updated ArchiveDocument
   */
  def updateArchiveDocument(
      fileId: FileId,
      ad: ArchiveDocument
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveDocument]] = {
    dmService.file(fileId).flatMap { maybeFile =>
      maybeFile.map { orig =>
        val updFile = for {
          // Place a lock on the file so the user can perform an update.
          locked <- MusitResultT(lockArchiveDocument(fileId))
          // Do the actual update
          updFid <- MusitResultT(updateDoc(orig, ad))
          // Remove the lock
          _ <- MusitResultT(unlockArchiveDocument(fileId))
          // Fetch the updated file
          updated <- MusitResultT(getArchiveDocument(fileId))
        } yield updated

        updFile.value.map { res =>
          // Make sure the lock is removed if the result is a MusitError
          if (res.isFailure) removeLock(fileId)
          res
        }.recover {
          case NonFatal(ex) =>
            log.error(
              s"There was a problem updating the file $fileId. Attempting to" +
                s"remove any locks that may have been placed by the operation.",
              ex
            )
            removeLock(fileId)
            MusitGeneralError(s"Could not be updated")
        }
      }.getOrElse {
        notFoundF(
          s"Unable to update ArchiveDocument $fileId because it doesn't exist"
        )
      }
    }
  }

  private[this] def updateDoc(
      origFile: ArchiveDocument,
      upd: ArchiveDocument
  )(implicit ac: ArchiveContext): Future[MusitResult[FileId]] = {
    val enriched = origFile.copy(
      description = upd.description,
      author = upd.author,
      collection = upd.collection,
      documentMedium = upd.documentMedium,
      documentDetails = upd.documentDetails,
      published = upd.published
    )
    dmService.updateFile(enriched).map {
      case Some(fid) =>
        MusitSuccess(fid)

      case None =>
        MusitGeneralError(
          s"Update of ${origFile.title} (${origFile.fid}) was unsuccessful"
        )
    }
  }

  private[this] def removeLock(
      fileId: FileId
  )(implicit ac: ArchiveContext): Unit = {
    dmService.unlockFile(fileId).onComplete {
      case scala.util.Success(unlocked) =>
        if (unlocked) log.debug(s"Successfully removed lock on $fileId after update.")
        else log.warn(s"Unable to remove lock from file $fileId after update.")

      case scala.util.Failure(msg) =>
        log.error(s"Unable to remove lock from file $fileId after update. Reason: $msg")

    }
  }

  /**
   * Tries to locate and return the ArchiveDocument with the specified FileId.
   *
   * @param fileId the FileId to look for
   * @param ac ArchiveContext
   * @return eventually a result that contain the located ArchiveDocument
   */
  def getArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[ArchiveDocument]] = {
    dmService.file(fileId).map {
      case Some(ad) => MusitSuccess(ad)
      case None     => MusitNotFound(s"Could not find ArchiveDocument $fileId")
    }
  }

  /**
   * Checks the given FileId for the presence of a Lock.
   *
   * @param fileId the FileId to check for a lock
   * @param ac ArchiveContext
   * @return eventually a result with a Boolean true if a Lock was found, else false
   */
  def isArchiveDocumentLocked(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.fileHasLock(fileId).map(MusitSuccess.apply)
  }

  /**
   * Tries to apply a Lock on the given FileId. This is typically necessary when
   * a user wants to prevent changes to the document. And when the user wants to
   * continue working on it, and upload new versions. A locked file cannot be
   * modified by other users.
   *
   * @param fileId the FileId to lock
   * @param ac ArchiveContext
   * @return eventually a result with the applied Lock if the operation was successful
   */
  def lockArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Lock]] = {
    dmService.lockFile(fileId).map {
      case Some(lock) => MusitSuccess(lock)
      case None       => MusitGeneralError(s"Lock was not applied to $fileId")
    }
  }

  /**
   * Tries to remove the Lock from the given FileId. Making it openly available
   * for other users to modify (move etc...).
   *
   * @param fileId the FileId to unlock
   * @param ac ArchiveContext
   * @return eventually a result with a Boolean true if lock was removed, else false
   */
  def unlockArchiveDocument(
      fileId: FileId
  )(implicit ac: ArchiveContext): Future[MusitResult[Boolean]] = {
    dmService.unlockFile(fileId).map(MusitSuccess.apply)
  }

  /**
   * Will try move the given FileId to the given destination FolderId.
   *
   * @param fileId the FileId to move
   * @param dest the FolderId of the new parent folder.
   * @param ac ArchiveContext
   * @return eventually a result with the updated ArchiveDocument
   */
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
