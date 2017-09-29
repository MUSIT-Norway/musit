package models.document

import models.document.ArchiveIdentifiers.ArchiveCollectionId
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.{AllowedParty, Owner}
import net.scalytica.symbiotic.api.types.{FileId, Lock, ManagedMetadata, Path}
import play.api.libs.json._

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

  // JSON key for type disambiguation
  val tpe = "type"

  implicit object ArchiveItemFormat extends Format[ArchiveItem] {

    override def reads(json: JsValue): JsResult[ArchiveItem] = {
      (json \ tpe)
        .asOpt[String]
        .map {
          case GenericFolder.FolderType =>
            GenericFolder.format.reads(json)

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
      case g: GenericFolder =>
        GenericFolder.format.writes(g).as[JsObject] ++ Json.obj(
          tpe -> GenericFolder.FolderType
        )

      case a: ArchiveFolderItem =>
        ArchiveFolderItem.ArchiveFolderItemFormat.writes(a)

      case a: ArchiveDocument =>
        ArchiveDocument.format.writes(a).as[JsObject] ++ Json.obj(
          tpe -> ArchiveDocument.DocType
        )
    }
  }

}
