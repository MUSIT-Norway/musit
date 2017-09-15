package models.document

import models.document.ArchiveIdentifiers._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.json.IgnoreJsPath
import net.scalytica.symbiotic.json.Implicits._
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
    case _: ArchiveRoot   => ArchiveRoot.FolderType
    case _: Archive       => Archive.FolderType
    case _: ArchivePart   => ArchivePart.FolderType
    case _: ArchiveFolder => ArchiveFolder.FolderType
  }

  object Implicits {

    implicit def managedFileToArchiveItem(mf: ManagedFile): ArchiveItem = {
      mf match {
        case f: Folder => folder2ArchiveFolderItem(f)
        case f: File   => f: ArchiveDocument
      }
    }

    implicit def managedFileSeqToArchiveItemSeq(
        smf: Seq[ManagedFile]
    ): Seq[ArchiveItem] = smf.map(mf => mf: ArchiveItem)

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
        case a: ArchiveRoot   => ArchiveRoot.archiveRoot2SymbioticRoot(a)
        case a: Archive       => Archive.archive2SymbioticFolder(a)
        case a: ArchivePart   => ArchivePart.archivePart2SymbioticFolder(a)
        case a: ArchiveFolder => ArchiveFolder.archiveFolder2SymbioticFolder(a)
      }
    }

  }

  /**
   * Defines base attributes required for any folder or document that are to be
   * stored in the document management / archive system.
   */
  trait ArchiveItem {

    val id: Option[ArchiveId] // unique identifier for file/folder (unique version)
    val fid: Option[FileId]   // Identifier for the file/folder (all versions)
    val title: String
    val description: Option[String]
    val owner: Option[Owner]
    val path: Option[Path]
    val lock: Option[Lock]

    //  val archiveStatus: ArchiveStatus  // can be derived
    val documentMedium: Option[String] // might change to a predefined ADT
    val createdStamp: Option[UserStamp]
    val published: Boolean

    val collection: Option[ArchiveCollectionId]

    // restrictions / "klausulering" here or in document?

    // val storageLocation: Option[StorageNodeId] ???

    /**
     * Provides a convenient mapping from the {{{ArchiveItem}}} to the necessary
     * {{{ManagedMetadata}}} type for the underlying document management library.
     *
     * @return [[MetadataMap]]
     */
    def managedMetadata: ManagedMetadata = {
      ManagedMetadata(
        owner = owner,
        accessibleBy = owner
          .map(o => AllowedParty(o.id))
          .toSeq ++ collection.map(p => AllowedParty(p)).toSeq,
        fid = fid,
        createdBy = createdStamp.map(_.by),
        isFolder = Some(true),
        path = path,
        description = description,
        lock = lock,
        extraAttributes = Some(metadataMap)
      )
    }

    def metadataMap: MetadataMap = {
      MetadataMap(
        "published"      -> published,
        "documentMedium" -> documentMedium
      )
    }

    def enrich()(implicit ctx: ArchiveAddContext): ArchiveItem

  }

  object ArchiveItem {

    implicit object ArchiveItemFormat extends Format[ArchiveItem] {
      private[this] val tpe = "type"

      override def reads(json: JsValue): JsResult[ArchiveItem] = {
        (json \ tpe)
          .asOpt[String]
          .map {
            case ArchiveDocument.DocType =>
              ArchiveDocument.format.reads(json)

            case _ =>
              ArchiveFolderItem.ArchiveFolderItemFormat.reads(json)
          }
          .getOrElse {
            JsError(s"Expected to find key $tpe with disambiguator for ArchiveItem")
          }
      }

      override def writes(o: ArchiveItem): JsValue = o match {
        case a: ArchiveFolderItem =>
          ArchiveFolderItem.ArchiveFolderItemFormat.writes(a)

        case a: ArchiveDocument =>
          ArchiveDocument.format.writes(a).as[JsObject] ++ Json.obj(
            tpe -> ArchiveDocument.DocType
          )
      }
    }

  }

  /**
   * Base abstraction for all types of folders that are stored in the document
   * management / archive system.
   */
  trait ArchiveFolderItem extends ArchiveItem {

    val closedStamp: Option[UserStamp]

    override def metadataMap: MetadataMap = {
      super.metadataMap ++ MetadataMap(
        "closedBy"   -> closedStamp.map(_.by.value),
        "closedDate" -> closedStamp.map(_.date)
      )
    }

    def isValidParentFor(fi: ArchiveFolderItem): Boolean

    def enrich()(implicit ctx: ArchiveAddContext): ArchiveFolderItem

    def updatePath(p: Path): ArchiveFolderItem

  }

  object ArchiveFolderItem {

    implicit object ArchiveFolderItemFormat extends Format[ArchiveFolderItem] {
      private[this] val tpe = "type"

      override def reads(json: JsValue): JsResult[ArchiveFolderItem] = {
        (json \ tpe)
          .asOpt[String]
          .map {
            case ArchiveRoot.FolderType   => ArchiveRoot.format.reads(json)
            case Archive.FolderType       => Archive.format.reads(json)
            case ArchivePart.FolderType   => ArchivePart.format.reads(json)
            case ArchiveFolder.FolderType => ArchiveFolder.format.reads(json)
            case wrong =>
              JsError(s"type $wrong is not a valid ArchiveFolderItem") // scalastyle:ignore
          }
          .getOrElse {
            JsError(s"Expected to find key $tpe with for ArchiveFolderItem")
          }
      }

      override def writes(o: ArchiveFolderItem): JsValue = o match {
        case a: ArchiveRoot =>
          ArchiveRoot.format.writes(a).as[JsObject] ++ Json.obj(
            tpe -> ArchiveRoot.FolderType
          )

        case a: Archive =>
          Archive.format.writes(a).as[JsObject] ++ Json.obj(
            tpe -> Archive.FolderType
          )

        case a: ArchivePart =>
          ArchivePart.format.writes(a).as[JsObject] ++ Json.obj(
            tpe -> ArchivePart.FolderType
          )

        case a: ArchiveFolder =>
          ArchiveFolder.format.writes(a).as[JsObject] ++ Json.obj(
            tpe -> ArchiveFolder.FolderType
          )
      }
    }

  }

  /**
   * An {{{ArchiveDocumentItem}}} is an abstraction on all physical files that are
   * uploaded into the system. It shares all the attributes with {{{ArchiveItem}}},
   * but also provides some attributes that are specific for a file. Of these,
   * the attribute {{{stream}}} is probably the most important one. This is where
   * you get a handle on the actual file, and is typically used to stream the file
   * back to the requesting party.
   *
   * {{{ArchiveDocumentItem}}}s also contain a {{{version}}} attribute. It is nothing
   * more than an automatically incremented {{{int}}}, that implies which version
   * of the associated physical file any given instance of {{{ArchiveDocumentItem}}}
   * describes.
   *
   * @see [[ArchiveItem]]
   */
  trait ArchiveDocumentItem extends ArchiveItem {

    val size: Option[String] // length
    val fileType: Option[String]
    val author: Option[String]
    val documentDetails: DocumentDetails
    val lock: Option[Lock]
    val version: Version
    // The reference to the actual _file_
    val stream: Option[FileStream]

    //  val storageLocation: Option[StorageNodeId] ???
    //  val referenceArchivePart ???
    //  val archiveReference????
    /*
    - TilknyttetRegistreringSom
    - tilknyttetDato
    - tilknyttetAv
     */

    override def managedMetadata: ManagedMetadata = {
      ManagedMetadata(
        owner = owner,
        accessibleBy = owner
          .map(o => AllowedParty(o.id))
          .toSeq ++ collection.map(p => AllowedParty(p)).toSeq,
        fid = fid,
        createdBy = createdStamp.map(_.by),
        isFolder = Some(false),
        path = path,
        description = description,
        lock = lock,
        extraAttributes = Some(metadataMap)
      )
    }

    override def metadataMap: MetadataMap =
      super.metadataMap ++ documentDetails ++ MetadataMap("author" -> author)

    def enrich()(implicit ctx: ArchiveAddContext): ArchiveDocumentItem

  }

  /**
   * Type for grouping together attributes that specifically describe an
   * {{{ArhiveDocumentItem}}}.
   *
   * @param number
   * @param docType
   * @param docSubType
   */
  case class DocumentDetails(
      number: Int = 1,
      docType: Option[String] = None,
      docSubType: Option[String] = None
      //  creator ???
  )

  object DocumentDetails {

    val DocumentNumberKey  = "documentNumber"
    val DocumentTypeKey    = "documentType"
    val DocumentSubTypeKey = "documentSubType"

    implicit val format: Format[DocumentDetails] = Json.format[DocumentDetails]

    implicit def fromMetadataMap(mdm: MetadataMap): DocumentDetails = DocumentDetails(
      mdm.getAs[Int](DocumentNumberKey).getOrElse(1),
      mdm.getAs[String](DocumentTypeKey),
      mdm.getAs[String](DocumentSubTypeKey)
    )

    implicit def fromOptMetadataMap(om: Option[MetadataMap]): DocumentDetails =
      om.map(fromMetadataMap).getOrElse(DocumentDetails())

    implicit def toMetadataMap(dd: DocumentDetails): MetadataMap = MetadataMap(
      DocumentNumberKey  -> dd.number,
      DocumentTypeKey    -> dd.docType,
      DocumentSubTypeKey -> dd.docSubType
    )

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

    val format: Format[ArchiveRoot] = Json.format[ArchiveRoot]

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

    val format: Format[Archive] = Json.format[Archive]

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
          date <- f.createdDate
          by   <- f.metadata.createdBy
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
      collection = ctx.collection,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    override def updatePath(p: Path) = this.copy(path = Some(p))

  }

  object ArchivePart {

    val FolderType = "archive_part"

    val format: Format[ArchivePart] = Json.format[ArchivePart]

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
          date <- f.createdDate
          by   <- f.metadata.createdBy
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
      collection = ctx.collection,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    override def updatePath(p: Path) = this.copy(path = Some(p))

  }

  object ArchiveFolder {

    val FolderType = "archive_folder"

    val format: Format[ArchiveFolder] = Json.format[ArchiveFolder]

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
          date <- f.createdDate
          by   <- f.metadata.createdBy
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
      collection = ctx.collection,
      createdStamp = Some(UserStamp(by = ctx.currentUser, date = dateTimeNow))
    )

    def updatePath(p: Path) = this.copy(path = Some(p))

  }

  object ArchiveDocument {

    val DocType = "archive_document"

    implicit def readsToIgnoreReads[T](r: JsPath): IgnoreJsPath = IgnoreJsPath(r)

    implicit val format: Format[ArchiveDocument] = (
      (__ \ "id").formatNullable[ArchiveId] and
        (__ \ "fid").formatNullable[FileId] and
        (__ \ "title").format[String] and
        (__ \ "size").formatNullable[String] and
        (__ \ "fileType").formatNullable[String] and
        (__ \ "description").formatNullable[String] and
        (__ \ "owner").formatNullable[Owner] and
        (__ \ "collection").formatNullable[ArchiveCollectionId] and
        (__ \ "path").formatNullable[Path] and
        (__ \ "lock").formatNullable[Lock] and
        (__ \ "version").format[Version] and
        (__ \ "published").format[Boolean] and
        (__ \ "documentMedium").formatNullable[String] and
        (__ \ "createdStamp").formatNullable[UserStamp] and
        (__ \ "author").formatNullable[String] and
        (__ \ "documentDetails").format[DocumentDetails] and
        (__ \ "stream").formatIgnore[FileStream]
    )(ArchiveDocument.apply, unlift(ArchiveDocument.unapply))

    implicit def archiveDoc2SymbioticFile(ad: ArchiveDocument): File = {
      File(
        id = ad.id,
        filename = ad.title,
        fileType = ad.fileType,
        createdDate = ad.createdStamp.map(_.date),
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
          date <- f.createdDate
          by   <- f.metadata.createdBy
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
