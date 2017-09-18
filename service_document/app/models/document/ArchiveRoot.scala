package models.document

import models.document.ArchiveIdentifiers.ArchiveCollectionId
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.{FileId, Folder, Lock, Path}
import net.scalytica.symbiotic.json.Implicits._
import play.api.libs.json.{Format, Json}

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
    case _: Archive       => true
    case _: GenericFolder => true
    case _                => false
  }

  override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
    owner = Some(ctx.owner)
  )

  override def updatePath(p: Path) = this

  override def typeString = ArchiveRoot.FolderType
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
