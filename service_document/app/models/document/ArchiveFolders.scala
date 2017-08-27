package models.document

import models.document.ArchiveItems._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

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
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem

  object Archive {

    val FolderType = "archive"

    implicit def archive2SymbioticFolder(a: Archive): Folder = {
      Folder(
        id = a.id,
        filename = a.title,
        fileType = Some(FolderType),
        createdDate = a.createdStamp.map(_.date),
        metadata = a.managedMetadata
      )
    }

    implicit def symbioticFolder2Archive(f: Folder): Archive = {
      val ea = f.metadata.extraAttributes
      Archive(
        id = f.id,
        fid = f.metadata.fid,
        title = f.filename,
        description = f.metadata.description,
        owner = f.metadata.owner,
        path = f.metadata.path,
        lock = f.metadata.lock,
        published = ea.flatMap(e => e.getAs[Boolean]("published")).getOrElse(false),
        documentMedium = ea.flatMap(e => e.getAs[String]("documentMedium")),
        closedStamp = for {
          date <- ea.flatMap(e => e.getAs[DateTime]("closedDate"))
          by   <- ea.flatMap(e => e.getAs[String]("closedBy")).map(ArchiveUserId.apply)
        } yield UserStamp(date, by),
        createdStamp = for {
          date <- f.uploadDate
          by   <- f.metadata.uploadedBy
        } yield UserStamp(date, by)
      )
    }

  }

  case class ArchivePart(
      id: Option[ArchiveId],
      fid: Option[FileId],
      title: String,
      description: Option[String],
      owner: Option[Owner],
      path: Option[Path],
      lock: Option[Lock],
      published: Boolean,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem

  object ArchivePart {

    val FolderType = "archive_part"

    implicit def archivePart2SymbioticFolder(a: ArchivePart): Folder = {
      Folder(
        id = a.id,
        filename = a.title,
        fileType = Some(FolderType),
        createdDate = a.createdStamp.map(_.date),
        metadata = a.managedMetadata
      )
    }

    implicit def symbioticFolder2ArchivePart(f: Folder): ArchivePart = {
      val ea = f.metadata.extraAttributes
      ArchivePart(
        id = f.id,
        fid = f.metadata.fid,
        title = f.filename,
        description = f.metadata.description,
        owner = f.metadata.owner,
        path = f.metadata.path,
        lock = f.metadata.lock,
        published = ea.flatMap(e => e.getAs[Boolean]("published")).getOrElse(false),
        documentMedium = ea.flatMap(e => e.getAs[String]("documentMedium")),
        closedStamp = for {
          date <- ea.flatMap(e => e.getAs[DateTime]("closedDate"))
          by   <- ea.flatMap(e => e.getAs[String]("closedBy")).map(ArchiveUserId.apply)
        } yield UserStamp(date, by),
        createdStamp = for {
          date <- f.uploadDate
          by   <- f.metadata.uploadedBy
        } yield UserStamp(date, by)
      )
    }
  }

  case class ArchiveFolder(
      id: Option[ArchiveId],
      fid: Option[FileId],
      title: String,
      description: Option[String],
      owner: Option[Owner],
      path: Option[Path],
      lock: Option[Lock],
      published: Boolean,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem

  object ArchiveFolder {

    val FolderType = "archive_folder"

    implicit def archiveFolder2SymbioticFolder(a: ArchiveFolder): Folder = {
      Folder(
        id = a.id,
        filename = a.title,
        fileType = Some(FolderType),
        createdDate = a.createdStamp.map(_.date),
        metadata = a.managedMetadata
      )
    }

    implicit def symbioticFolder2ArchiveFolder(f: Folder): ArchiveFolder = {
      val ea = f.metadata.extraAttributes
      ArchiveFolder(
        id = f.id,
        fid = f.metadata.fid,
        title = f.filename,
        description = f.metadata.description,
        owner = f.metadata.owner,
        path = f.metadata.path,
        lock = f.metadata.lock,
        published = ea.flatMap(e => e.getAs[Boolean]("published")).getOrElse(false),
        documentMedium = ea.flatMap(e => e.getAs[String]("documentMedium")),
        closedStamp = for {
          date <- ea.flatMap(e => e.getAs[DateTime]("closedDate"))
          by   <- ea.flatMap(e => e.getAs[String]("closedBy")).map(ArchiveUserId.apply)
        } yield UserStamp(date, by),
        createdStamp = for {
          date <- f.uploadDate
          by   <- f.metadata.uploadedBy
        } yield UserStamp(date, by)
      )
    }

  }

}
