package models.document
import models.document.ArchiveStatuses.ArchiveStatus
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._
import CustomMetadataAttributes.Implicits._

object ArchiveDocuments {

  case class ArchiveDocument(
      id: Option[ArchiveId],
      fid: Option[FileId],
      title: String,
      size: Option[String],
      fileType: Option[String],
      description: Option[String],
      owner: Option[Owner],
      path: Option[Path],
      lock: Option[Lock],
      version: Version,
      published: Boolean,
      archiveStatus: ArchiveStatus,
      documentMedium: Option[String],
      closedStamp: Option[UserStamp],
      createdStamp: Option[UserStamp],
      author: Option[String],
      documentDetails: Option[DocumentDetails],
      stream: Option[FileStream]
  ) extends ArchiveDocumentItem

  object ArchiveDocument {

    private[this] def metadataMapFrom(ad: ArchiveDocument): MetadataMap = {
      MetadataMap(
        "published"       -> ad.published,
        "documentMedium"  -> ad.documentMedium,
        "closedBy"        -> ad.closedStamp.map(_.by.value),
        "closedDate"      -> ad.closedStamp.map(_.date),
        "author"          -> ad.author,
        "documentNumber"  -> ad.documentDetails.map(_.number),
        "documentType"    -> ad.documentDetails.flatMap(_.docType),
        "documentSubType" -> ad.documentDetails.flatMap(_.docSubType)
      )
    }

    private[this] def managedMetadataFrom(ad: ArchiveDocument): ManagedMetadata = {
      ManagedMetadata(
        owner = ad.owner,
        fid = ad.fid,
        uploadedBy = ad.createdStamp.map(_.by),
        version = ad.version,
        isFolder = Some(false),
        path = ad.path,
        description = ad.description,
        lock = ad.lock,
        extraAttributes = Some(metadataMapFrom(ad))
      )
    }

    implicit def archiveDoc2SymbioticFile(ad: ArchiveDocument): File = {
      File(
        id = ad.id,
        filename = ad.title,
        fileType = ad.fileType,
        uploadDate = ad.createdStamp.map(_.date),
        length = ad.size,
        stream = ad.stream,
        metadata = managedMetadataFrom(ad)
      )
    }

    implicit def symbioticFile2ArchiveDoc(f: File): ArchiveDocument = {
      ArchiveDocument(
        id = f.id,
        fid = f.metadata.fid,
        title = f.filename,
        size = f.length,
        fileType = f.fileType,
        description = f.metadata.description,
        owner = f.metadata.owner,
        path = f.metadata.path,
        lock = f.metadata.lock,
        version = f.metadata.version,
        published = ???,
        archiveStatus = ???,
        documentMedium = ???,
        closedStamp = ???,
        createdStamp = ???,
        author = ???,
        documentDetails = ???,
        stream = f.stream
      )
    }

  }

}
