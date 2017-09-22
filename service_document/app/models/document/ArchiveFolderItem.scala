package models.document

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.Path
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.json.Implicits._
import play.api.libs.json._

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

  def updatePath(p: Path): ArchiveFolderItem

  def typeString: String

}

object ArchiveFolderItem {

  implicit object ArchiveFolderItemFormat extends Format[ArchiveFolderItem] {
    private[this] val tpe = "type"

    override def reads(json: JsValue): JsResult[ArchiveFolderItem] = {
      (json \ tpe)
        .asOpt[String]
        .map {
          case ArchiveRoot.FolderType   => ArchiveRoot.format.reads(json)
          case Archive.FolderType       => Archive.format.reads(json)
          case ArchivePart.FolderType   => ArchivePart.format.reads(json)
          case ArchiveFolder.FolderType => ArchiveFolder.format.reads(json)
          case wrong =>
            JsError(s"type $wrong is not a valid ArchiveFolderItem")
        }
        .getOrElse {
          JsError(s"Expected to find key $tpe with for ArchiveFolderItem")
        }
    }

    override def writes(o: ArchiveFolderItem): JsValue = o match {
      case a: ArchiveRoot =>
        ArchiveRoot.format.writes(a).as[JsObject] ++ Json.obj(
          "path" -> Json.toJson(Path.root),
          tpe    -> ArchiveRoot.FolderType
        )

      case a: Archive =>
        Archive.format.writes(a).as[JsObject] ++ Json.obj(
          tpe -> Archive.FolderType
        )

      case a: ArchivePart =>
        ArchivePart.format.writes(a).as[JsObject] ++ Json.obj(
          tpe -> ArchivePart.FolderType
        )

      case a: ArchiveFolder =>
        ArchiveFolder.format.writes(a).as[JsObject] ++ Json.obj(
          tpe -> ArchiveFolder.FolderType
        )
    }
  }

}
