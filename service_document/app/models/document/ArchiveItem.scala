package models.document

import models.document.ArchiveStatuses.ArchiveStatus
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._

trait ArchiveItem {

  val id: Option[ArchiveId] // unique identifier for file/folder (unique version)
  val fid: Option[FileId]   // Identifier for the file/folder (all versions)
  val title: String
  val description: Option[String]
  val owner: Option[Owner]
  val path: Option[Path]
  val lock: Option[Lock]

  val archiveStatus: ArchiveStatus
  val documentMedium: Option[String] // might change to a predefined ADT
  val createdStamp: Option[UserStamp]
  val published: Boolean

  // restrictions / "klausulering" here or in document?

  // val storageLocation: Option[StorageNodeId] ???

}

trait ArchiveFolderItem extends ArchiveItem {

  val closedStamp: Option[UserStamp]

}

case class DocumentDetails(
    number: Int = 1,
    docType: Option[String],
    docSubType: Option[String]
//  creator ???
)

trait ArchiveDocumentItem extends ArchiveItem {

  val size: Option[String] // length
  val fileType: Option[String]
  val author: Option[String]
  val documentDetails: Option[DocumentDetails]
  val lock: Option[Lock]
  val version: Version
  // The reference to the actual _file_
  val stream: Option[FileStream]

//  val storageLocation: Option[StorageNodeId] ???
//  val referenceArchivePart ???
//  val archiveReference????
  /*
    o TilknyttetRegistreringSom
    o tilknyttetDato
    o tilknyttetAv
 */

}
