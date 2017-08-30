package models.document

import models.document.ArchiveIdentifiers._
import models.document.ArchiveItems._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._
import org.joda.time.DateTime

object ArchiveFolders {

  object Implicits {
    def folderAsArchiveFolderItem(f: Folder): Option[ArchiveFolderItem] =
      f.fileType.flatMap {
        case Archive.FolderType       => Some(f: Archive)
        case ArchivePart.FolderType   => Some(f: ArchivePart)
        case ArchiveFolder.FolderType => Some(f: ArchiveFolder)
        case _                        => None
      }

    @throws(classOf[IllegalArgumentException])
    implicit def folder2ArchiveFolderItem(f: Folder): ArchiveFolderItem = {
      folderAsArchiveFolderItem(f).getOrElse(
        throw new IllegalArgumentException(
          "The folder cannot be converted to an ArchiveFolderItem because " +
            s"folder type is ${f.fileType}"
        )
      )
    }

    implicit def optFolder2ArchiveFolderItem(
        of: Option[Folder]
    ): Option[ArchiveFolderItem] = of.flatMap(folderAsArchiveFolderItem)

    implicit def archiveFolderItemAsFolder(afi: ArchiveFolderItem): Folder = {
      afi match {
        case a: Archive        => a: Folder
        case ap: ArchivePart   => ap: Folder
        case af: ArchiveFolder => af: Folder
      }
    }
  }

  /**
   * Each Museum can 1 and only 1 Archive folder. It is the basis of the entire
   * archive structure for a given museum. An Archive node will be the first
   * and only folder in the folder-tree that has a symbiotic {{{Root}}} folder
   * as its parent.
   */
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
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case ap: ArchivePart => true
      case afi             => false
    }

  }

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

  /**
   * An {{{Archive}}} can _only_ consist of {{{ArchivePart}}} folders.
   */
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
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case afi: ArchiveFolder      => true
      case ad: ArchiveDocumentItem => true
      case _                       => false
    }

  }

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

  /**
   * An {{{ArchiveFolder}}} is a general purpose folder type used to build
   * folder hierarchies below an {{{ArchivePart}}}. It can itself contain other
   * {{{ArchiveFolder}}}s and {{{ArchiveDocument}}}s.
   */
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
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case afi: ArchiveFolder      => true
      case ad: ArchiveDocumentItem => true
      case _                       => false
    }

  }

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
