package no.uio.musit.microservice.storageAdmin.domain

import enumeratum.EnumEntry.Lowercase
import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }

sealed trait StorageType extends EnumEntry

object StorageType extends Enum[StorageType] with PlayJsonEnum[StorageType] {
  val values = findValues
  case object StorageUnit extends StorageType with Lowercase
  case object Room extends StorageType with Lowercase
  case object Building extends StorageType with Lowercase
}
