package models.document

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Type for grouping together attributes that specifically describe an
 * {{{ArhiveDocumentItem}}}.
 */
case class DocumentDetails(
    number: Int = 1,
    docType: Option[String] = None,
    docSubType: Option[String] = None
    //  creator ???
)

object DocumentDetails {

  val DocumentNumberKey  = "documentNumber"
  val DocumentTypeKey    = "documentType"
  val DocumentSubTypeKey = "documentSubType"

  implicit val format: Format[DocumentDetails] = (
    (__ \ "number").formatNullable[Int].inmap[Int](_.getOrElse(1), Option.apply) and
      (__ \ "docType").formatNullable[String] and
      (__ \ "docSubType").formatNullable[String]
  )(DocumentDetails.apply, unlift(DocumentDetails.unapply))

  implicit def fromMetadataMap(mdm: MetadataMap): DocumentDetails = DocumentDetails(
    mdm.getAs[Int](DocumentNumberKey).getOrElse(1),
    mdm.getAs[String](DocumentTypeKey),
    mdm.getAs[String](DocumentSubTypeKey)
  )

  implicit def fromOptMetadataMap(om: Option[MetadataMap]): DocumentDetails =
    om.map(fromMetadataMap).getOrElse(DocumentDetails())

  implicit def toMetadataMap(dd: DocumentDetails): MetadataMap = MetadataMap(
    DocumentNumberKey  -> dd.number,
    DocumentTypeKey    -> dd.docType,
    DocumentSubTypeKey -> dd.docSubType
  )

}
