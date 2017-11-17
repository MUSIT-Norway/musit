package models.document

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.ResourceParties.AllowedParty
import net.scalytica.symbiotic.api.types.{FileStream, Lock, ManagedMetadata, Version}

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
