package no.uio.musit.microservice.storageAdmin.domain

import no.uio.musit.microservices.common.extensions.FutureExtensions.MusitResult
import play.api.libs.json.{ Format, Json }

/**
 * Created by ellenjo on 5/19/16.
 */
sealed trait StorageUnitType {
  def typename: String

}

object StorageUnitType {
  def apply(stType: String): Option[StorageUnitType] = stType.toLowerCase match {
    case "building" => Some(Building)
    case "room" => Some(Room)
    case "storageunit" => Some(StUnit)
    case _ => None //throw new Exception(s"Musit: Undefined StorageType:$other")
  }

}

object Room extends StorageUnitType {
  def typename = "room"
}

object Building extends StorageUnitType {
  def typename = "building"
}

object StUnit extends StorageUnitType {
  def typename = "storageunit"
}
