package no.uio.musit.microservice.storageAdmin.domain.dto

import no.uio.musit.microservice.storageAdmin.domain.{ Building, Room, Storage, StorageUnit, Organisation }

sealed trait StorageType

object StorageType {

  def toString[T <: StorageType](st: T): String = st match {
    case StorageType.Building => "Building"
    case StorageType.Room => "Room"
    case StorageType.StorageUnit => "StorageUnit"
    case StorageType.Organisation => "Organisation"
  }

  def fromString(st: String): StorageType = st match {
    case "Building" => StorageType.Building
    case "Room" => StorageType.Room
    case "StorageUnit" => StorageType.StorageUnit
    case "Organisation" => StorageType.Organisation
    case "Root" => StorageType.Organisation
  }

  def fromStorage[T <: Storage](st: T): StorageType = st match {
    case _: StorageUnit => StorageType.StorageUnit
    case _: Building => StorageType.Building
    case _: Room => StorageType.Room
    case _: Organisation => StorageType.Organisation
  }

  case object StorageUnit extends StorageType

  case object Building extends StorageType

  case object Room extends StorageType

  case object Organisation extends StorageType

}
