package models.analysis

import play.api.libs.json.{Format, Json}

case class StorageContainer(
    storageContainerId: Int,
    noStorageContainer: String,
    enStorageContainer: String
)

object StorageContainer {
  implicit val format: Format[StorageContainer] = Json.format[StorageContainer]
}
