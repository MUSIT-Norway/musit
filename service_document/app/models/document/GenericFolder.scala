package models.document

import models.document.ArchiveIdentifiers.ArchiveCollectionId
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.Owner
import net.scalytica.symbiotic.api.types.{FileId, Folder, Lock, Path}
import net.scalytica.symbiotic.json.Implicits._
import play.api.libs.json.{Format, Json}

/**
 * Encoding to represent the {{{GenericFolder}}} folder type. It can live
 * in it's own nested tree structure below the {{{ArchiveRoot}}} node. It is
 * typically used to build a folder tree where MUSIT modules can upload their
 * attachments.
 */
case class GenericFolder(
    id: Option[ArchiveId],
    fid: Option[FileId],
    title: String,
    owner: Option[Owner],
    path: Option[Path]
) extends ArchiveFolderItem {

  override val description: Option[String] = None
  override val collection: Option[ArchiveCollectionId] = None
  override val lock: Option[Lock] = None
  override val published: Boolean = false
  override val documentMedium: Option[String] = None
  override val closedStamp: Option[UserStamp] = None
  override val createdStamp: Option[UserStamp] = None

  override def isValidParentFor(fi: ArchiveFolderItem): Boolean = fi match {
    case _: GenericFolder => true
    case _ => false
  }

  override def enrich()(implicit ctx: ArchiveAddContext) = this.copy(
    owner = Some(ctx.owner)
  )

  override def updatePath(p: Path) = this.copy(path = Some(p))

  override def typeString = GenericFolder.FolderType
}

object GenericFolder {
  val FolderType = "generic_folder"

  def apply(path: Path): GenericFolder = GenericFolder(
    id = None,
    fid = None,
    title = path.nameOfLast,
    owner = None,
    path = Some(path)
  )

  val format: Format[GenericFolder] = Json.format[GenericFolder]

  implicit def genericFolder2SymbioticFolder(a: GenericFolder): Folder = {
    Folder(
      id = a.id,
      filename = a.title,
      fileType = Some(FolderType),
      metadata = a.managedMetadata
    )
  }

  implicit def symbioticFolder2GenericFolder(f: Folder): GenericFolder = {
    GenericFolder(
      id = f.id,
      fid = f.metadata.fid,
      title = f.filename,
      owner = f.metadata.owner,
      path = f.metadata.path
    )
  }
}
