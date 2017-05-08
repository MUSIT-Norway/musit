package models.analysis

import play.api.libs.json.{Format, Json}

case class StorageMedium(
    storageMediumId: Int,
    noStorageMedium: String,
    enStorageMedium: String
)

object StorageMedium {
  implicit val format: Format[StorageMedium] = Json.format[StorageMedium]
}
