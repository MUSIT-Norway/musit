package models.document

import models.document.ArchiveIdentifiers.ArchiveCollectionId
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import net.scalytica.symbiotic.api.types._

object Archiveables {

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
          .toSeq ++ collection.map(p => AllowedParty(p))
          .toSeq,
        fid = fid,
        uploadedBy = createdStamp.map(_.by),
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
        accessibleBy = collection.map(p => AllowedParty(p)).toSeq,
        fid = fid,
        uploadedBy = createdStamp.map(_.by),
        isFolder = Some(false),
        path = path,
        description = description,
        lock = lock,
        extraAttributes = Some(metadataMap)
      )
    }

    override def metadataMap: MetadataMap =
      super.metadataMap ++
        documentDetails ++
        MetadataMap("author" -> author)

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

}
