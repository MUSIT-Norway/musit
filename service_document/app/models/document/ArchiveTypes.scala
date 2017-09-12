package models.document

import models.document.ArchiveIdentifiers._
import models.document.Archiveables._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types._
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime

object ArchiveTypes {

  def folderAsArchiveFolderItem(f: Folder): Option[ArchiveFolderItem] = {
    f.fileType.flatMap {
      case Archive.FolderType       => Some(f: Archive)
      case ArchivePart.FolderType   => Some(f: ArchivePart)
      case ArchiveFolder.FolderType => Some(f: ArchiveFolder)
      case _                        => None
    }.orElse {
      if (Path.root == f.flattenPath) Some(f: ArchiveRoot)
      else None
    }
  }

  def asTypeString(a: ArchiveFolderItem): String = a match {
    case ar: ArchiveRoot   => ArchiveRoot.FolderType
    case a: Archive        => Archive.FolderType
    case ap: ArchivePart   => ArchivePart.FolderType
    case af: ArchiveFolder => ArchiveFolder.FolderType
  }

  object Implicits {

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
        case ar: ArchiveRoot   => ArchiveRoot.archiveRoot2SymbioticRoot(ar)
        case ac: Archive       => Archive.archive2SymbioticFolder(ac)
        case ap: ArchivePart   => ArchivePart.archivePart2SymbioticFolder(ap)
        case af: ArchiveFolder => ArchiveFolder.archiveFolder2SymbioticFolder(af)
      }
    }

    implicit def managedFileToArchiveItem(mf: ManagedFile): ArchiveItem = {
      mf match {
        case f: Folder => folder2ArchiveFolderItem(f)
        case f: File   => f: ArchiveDocument
      }
    }

    implicit def managedFileSeqToArchiveItemSeq(
        smf: Seq[ManagedFile]
    ): Seq[ArchiveItem] = smf.map(mf => mf: ArchiveItem)
  }

  case class ArchiveRoot(
      id: Option[ArchiveId],
      fid: Option[FileId],
      owner: Option[Owner]
  ) extends ArchiveFolderItem {
    val title: String                           = ArchiveRoot.FolderType
    val description: Option[String]             = None
    val collection: Option[ArchiveCollectionId] = None
    val path: Option[Path]                      = Some(Path.root)
    val lock: Option[Lock]                      = None
    val published: Boolean                      = false
    val documentMedium: Option[String]          = None
    val closedStamp: Option[UserStamp]          = None
    val createdStamp: Option[UserStamp]         = None

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case _: Archive => true
      case _          => false
    }

    override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
      owner = Some(ctx.owner)
    )

    override def updatePath(p: Path) = this
  }

  object ArchiveRoot {

    val FolderType = "root"

    implicit def archiveRoot2SymbioticRoot(ar: ArchiveRoot): Folder = {
      Folder(
        id = ar.id,
        filename = ar.title,
        metadata = ar.managedMetadata
      )
    }

    implicit def symbioticFolder2ArchiveRoot(f: Folder): ArchiveRoot = {
      ArchiveRoot(
        id = f.id,
        fid = f.metadata.fid,
        owner = f.metadata.owner
      )
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
      collection: Option[ArchiveCollectionId],
      path: Option[Path], // must include its own title as the last element!
      lock: Option[Lock],
      published: Boolean,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case _: ArchivePart => true
      case _              => false
    }

    override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
      owner = Some(ctx.owner),
      collection = ctx.collection,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    override def updatePath(p: Path) = this.copy(path = Some(p))

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
        collection = f.metadata.accessibleBy.tail.map(_.id).headOption,
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
      collection: Option[ArchiveCollectionId],
      path: Option[Path], // must include its own title as the last element!
      lock: Option[Lock],
      published: Boolean,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case _: ArchiveFolder       => true
      case _: ArchiveDocumentItem => true
      case _                      => false
    }

    override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
      owner = Some(ctx.owner),
      collection = ctx.accessibleParties.headOption,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    override def updatePath(p: Path) = this.copy(path = Some(p))

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
        collection = f.metadata.accessibleBy.tail.map(_.id).headOption,
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
      collection: Option[ArchiveCollectionId],
      path: Option[Path], // must include its own title as the last element!
      lock: Option[Lock],
      published: Boolean,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp]
  ) extends ArchiveFolderItem {

    override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
      case _: ArchiveFolder       => true
      case _: ArchiveDocumentItem => true
      case _                      => false
    }

    override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
      owner = Some(ctx.owner),
      collection = ctx.accessibleParties.headOption,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    override def updatePath(p: Path) = this.copy(path = Some(p))

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
        collection = f.metadata.accessibleBy.tail.map(_.id).headOption,
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
   * The {{{ArchiveDocument}}} represents an actual, uploaded, file. Including
   * a reference to the physical location where the file can be found (the
   * stream attribute).
   */
  case class ArchiveDocument(
      id: Option[ArchiveId],
      fid: Option[FileId],
      title: String,
      size: Option[String],
      fileType: Option[String],
      description: Option[String],
      owner: Option[Owner],
      collection: Option[ArchiveCollectionId],
      path: Option[Path], // must NOT include its own title as the last element!
      lock: Option[Lock],
      version: Version,
      published: Boolean,
      documentMedium: Option[String],
      createdStamp: Option[UserStamp],
      author: Option[String],
      documentDetails: DocumentDetails,
      stream: Option[FileStream]
  ) extends ArchiveDocumentItem {

    override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
      owner = Some(ctx.owner),
      collection = ctx.accessibleParties.headOption,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

  }

  object ArchiveDocument {

    implicit def archiveDoc2SymbioticFile(ad: ArchiveDocument): File = {
      File(
        id = ad.id,
        filename = ad.title,
        fileType = ad.fileType,
        uploadDate = ad.createdStamp.map(_.date),
        length = ad.size,
        stream = ad.stream,
        metadata = ad.managedMetadata
      )
    }

    implicit def symbioticFile2ArchiveDoc(f: File): ArchiveDocument = {
      val ea = f.metadata.extraAttributes
      ArchiveDocument(
        id = f.id,
        fid = f.metadata.fid,
        title = f.filename,
        size = f.length,
        fileType = f.fileType,
        description = f.metadata.description,
        owner = f.metadata.owner,
        collection = f.metadata.accessibleBy.tail.map(_.id).headOption,
        path = f.metadata.path,
        lock = f.metadata.lock,
        version = f.metadata.version,
        published = ea.flatMap(e => e.getAs[Boolean]("published")).getOrElse(false),
        documentMedium = ea.flatMap(e => e.getAs[String]("documentMedium")),
        createdStamp = for {
          date <- f.uploadDate
          by   <- f.metadata.uploadedBy
        } yield UserStamp(date, by),
        author = ea.flatMap(e => e.getAs[String]("author")),
        documentDetails = ea,
        stream = f.stream
      )
    }

    implicit def optFile2OptArchiveDoc(mf: Option[File]): Option[ArchiveDocument] = {
      mf.map(f => f: ArchiveDocument)
    }

  }

}
