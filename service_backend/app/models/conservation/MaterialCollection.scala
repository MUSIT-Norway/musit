package models.conservation

import play.api.libs.json.{Format, Json}

case class MaterialCollection(
    materialId: Int,
    collectionId: Int
)

object MaterialCollection {
  implicit val format: Format[MaterialCollection] = Json.format[MaterialCollection]
}
sealed trait MaterialBase {
  val materialId: Int
  val noMaterial: String
  val enMaterial: Option[String]
}

case class MaterialArchaeology(
    materialId: Int,
    noMaterial: String,
    enMaterial: Option[String]
) extends MaterialBase

object MaterialArchaeology {
  implicit val format: Format[MaterialArchaeology] = Json.format[MaterialArchaeology]
}

case class MaterialNumismatic(
    materialId: Int,
    noMaterial: String,
    enMaterial: Option[String]
) extends MaterialBase

object MaterialNumismatic {
  implicit val format: Format[MaterialNumismatic] = Json.format[MaterialNumismatic]
}

case class MaterialEthnography(
    materialId: Int,
    noMaterial: String,
    noMaterialType: Option[String],
    noMaterial_element: Option[String],
    enMaterial: Option[String],
    enMaterial_type: Option[String],
    enMaterial_element: Option[String],
    frMaterial: Option[String],
    frMaterial_type: Option[String],
    frMaterial_element: Option[String]
) extends MaterialBase

object MaterialEthnography {
  implicit val format: Format[MaterialEthnography] = Json.format[MaterialEthnography]
}
