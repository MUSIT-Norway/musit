package no.uio.musit.microservice.storageAdmin.domain.dto

import no.uio.musit.microservice.storageAdmin.domain.{ Building, Room, Storage, StorageUnit }

sealed trait StorageType

object StorageType {

  def toString[T <: StorageType](st: T): String = st match {
    case StorageType.Building => "Building"
    case StorageType.Room => "Room"
    case StorageType.StorageUnit => "StorageUnit"
  }

  def fromString(st: String): StorageType = st match {
    case "Building" => StorageType.Building
    case "Room" => StorageType.Room
    case "StorageUnit" => StorageType.StorageUnit
  }

  def fromStorage[T <: Storage](st: T): StorageType = st match {
    case _: StorageUnit => StorageType.StorageUnit
    case _: Building => StorageType.Building
    case _: Room => StorageType.Room
  }

  case object StorageUnit extends StorageType

  case object Building extends StorageType

  case object Room extends StorageType

}
