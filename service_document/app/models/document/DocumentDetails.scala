package models.document

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import play.api.libs.json.{Format, Json}

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

  implicit val format: Format[DocumentDetails] = Json.format[DocumentDetails]

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
