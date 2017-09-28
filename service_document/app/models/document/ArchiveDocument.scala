package models.document

import models.document.ArchiveIdentifiers.ArchiveCollectionId
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types._
import net.scalytica.symbiotic.json.IgnoreJsPath
import net.scalytica.symbiotic.json.Implicits._
import no.uio.musit.time.dateTimeNow
import play.api.libs.functional.syntax._
import play.api.libs.json._

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

  def updateMetadata(ad: ArchiveDocument): ArchiveDocument = {
    this.copy(
      description = ad.description,
      author = ad.author,
      collection = ad.collection,
      documentMedium = ad.documentMedium,
      documentDetails = ad.documentDetails,
      published = ad.published
    )
  }

  def updatePath(p: Path) = this.copy(path = Some(p))

}

object ArchiveDocument {

  def apply(
      title: String,
      fileType: Option[String],
      fileSize: Option[String],
      stream: Option[FileStream]
  ): ArchiveDocument = {
    ArchiveDocument(
      id = None,
      fid = None,
      title = title,
      size = fileSize,
      fileType = fileType,
      description = None,
      owner = None,
      collection = None,
      path = None,
      lock = None,
      version = 1,
      published = false,
      documentMedium = None,
      createdStamp = None,
      author = None,
      documentDetails = None,
      stream = stream
    )
  }

  val DocType = "archive_document"

  implicit def readsToIgnoreReads[T](r: JsPath): IgnoreJsPath = IgnoreJsPath(r)

  val format: Format[ArchiveDocument] = (
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

  implicit val reads: Reads[ArchiveDocument] = Reads { jsv =>
    (jsv \ ArchiveItem.tpe)
      .asOpt[String]
      .map {
        case ArchiveDocument.DocType =>
          format.reads(jsv)

        case wrong =>
          JsError(s"type $wrong is not valid for ArchiveDocument")
      }
      .getOrElse {
        JsError(
          s"Expected to find key ${ArchiveItem.tpe} with for ArchiveDocument"
        )
      }
  }

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
