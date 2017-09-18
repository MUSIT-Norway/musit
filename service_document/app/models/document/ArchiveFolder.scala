package models.document

import models.document.ArchiveIdentifiers.{ArchiveCollectionId, ArchiveUserId}
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.{FileId, Folder, Lock, Path}
import net.scalytica.symbiotic.json.Implicits._
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

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

  override def typeString = ArchiveFolder.FolderType
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
