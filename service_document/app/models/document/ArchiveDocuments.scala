package models.document

import models.document.ArchiveItems._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types._

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
      documentMedium: Option[String],
      createdStamp: Option[UserStamp],
      author: Option[String],
      documentDetails: DocumentDetails,
      stream: Option[FileStream]
  ) extends ArchiveDocumentItem

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

  }

}
