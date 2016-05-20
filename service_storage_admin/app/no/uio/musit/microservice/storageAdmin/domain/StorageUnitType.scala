package no.uio.musit.microservice.storageAdmin.domain

import play.api.libs.json.{Format, Json}


/**
  * Created by ellenjo on 5/19/16.
  */
sealed trait StorageUnitType

object StorageUnitType{
  def apply(stType: String) = stType match{
    case "building" => Building
    case "room" => Room
    case "storageUnit" => StorageUnit
    case "rootNode" => RootNode
  }
 // implicit val format: Format[StorageUnitType] = Json.format[StorageUnitType]
}

object Room extends StorageUnitType

object Building extends StorageUnitType

object StUnit extends StorageUnitType

object RootNode extends StorageUnitType