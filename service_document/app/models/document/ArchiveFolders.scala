package models.document

import models.document.ArchiveStatuses._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._


object ArchiveFolders {

  case class Archive(
    id: Option[ArchiveId],
    fid: Option[FileId],
    title: String,
    description: Option[String],
    owner: Option[Owner],
    path: Option[Path],
    lock: Option[Lock],
    published: Boolean,
    archiveStatus: ArchiveStatus,
    documentMedium: Option[String],
    closedStamp: Option[UserStamp],
    createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem

  case class ArchivePart(
    id: Option[ArchiveId],
    fid: Option[FileId],
    title: String,
    description: Option[String],
    owner: Option[Owner],
    path: Option[Path],
    lock: Option[Lock],
    published: Boolean,
    archiveStatus: ArchiveStatus,
    documentMedium: Option[String],
    closedStamp: Option[UserStamp],
    createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem

  case class ArchiveFolder(
    id: Option[ArchiveId],
    fid: Option[FileId],
    title: String,
    description: Option[String],
    owner: Option[Owner],
    path: Option[Path],
    lock: Option[Lock],
    published: Boolean,
    archiveStatus: ArchiveStatus,
    documentMedium: Option[String],
    closedStamp: Option[UserStamp],
    createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem


//  case class CaseFolder() extends ArchiveFolderItem

}