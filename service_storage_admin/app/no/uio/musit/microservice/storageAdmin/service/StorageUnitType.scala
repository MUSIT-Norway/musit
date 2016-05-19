package no.uio.musit.microservice.storageAdmin.service

/**
  * Created by ellenjo on 5/19/16.
  */
sealed trait StorageUnitType

object Room extends StorageUnitType

object Building extends StorageUnitType