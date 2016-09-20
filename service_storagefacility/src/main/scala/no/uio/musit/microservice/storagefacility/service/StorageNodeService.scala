/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.DummyData
import no.uio.musit.microservice.storagefacility.dao.storage.{ BuildingDao, OrganisationDao, RoomDao, StorageUnitDao }
import no.uio.musit.microservice.storagefacility.domain.MusitResults._
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageNodeDto
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject() (
    val storageUnitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val organisationDao: OrganisationDao,
    val envReqService: EnvironmentRequirementService
) {

  val logger = Logger(classOf[StorageNodeService])

  private[service] def saveEnvReq(
    nodeId: StorageNodeId,
    envReq: EnvironmentRequirement
  )(implicit currUsr: String) = {
    val now = dateTimeNow

    val er = EnvRequirement(
      baseEvent = MusitEventBase(
        id = None,
        doneBy = Some(ActorRole(1, DummyData.DummyUserId)), // FIXME: DO NOT FORGET TO CHANGE THIS!!!
        doneDate = now,
        note = envReq.comments,
        partOf = None,
        affectedThing = Some(ObjectRole(
          roleId = 1, // TODO: This should be inserted in DB on bootstrapping if not exists.
          objectId = nodeId
        )),
        registeredBy = Some(currUsr),
        registeredDate = Some(now)
      ),
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = envReq.temperature,
      airHumidity = envReq.relativeHumidity,
      hypoxicAir = envReq.hypoxicAir,
      cleaning = envReq.cleaning,
      light = envReq.lightingCondition
    )
    envReqService.add(er).map {
      case MusitSuccess(success) =>
        logger.debug("Successfully wrote environment requirement data " +
          s"for node $nodeId")
      case err: MusitError[_] =>
        logger.error("Something went wrong while storing the environment " +
          s"requirements for node $nodeId")
    }
  }

  /**
   * Helper function for storing a storage node. It will first try to persist
   * the storage node. If successful it will persist the environment requirement
   * event if and only if the event contains data.
   *
   * @param node    the StorageNode to persist
   * @param persist a function that persists a StorageNode and returns a Future
   *                value of the StorageNode enriched with the newly assigned ID.
   * @param currUsr implicitly scoped current user.
   * @tparam T the function will only work on any type T that is a sub-class of
   *           StorageNode.
   * @return The newly created StorageNode enriched with the assigned ID.
   */
  private def addStorageNode[T <: StorageNode](node: T)(
    persist: T => Future[T]
  )(implicit currUsr: String): Future[T] = {
    persist(node).map { sn =>
      for {
        nodeId <- sn.id
        envReq <- sn.environmentRequirement
      } yield {
        logger.info(s"Saving new environment requirement data for node $nodeId")
        saveEnvReq(nodeId, envReq)
      }
      sn
    }
  }

  private def updateStorageNode[T <: StorageNode](id: StorageNodeId, node: T)(
    persist: (StorageNodeId, T) => Future[Option[T]]
  )(implicit currUsr: String): Future[MusitResult[Option[T]]] = {
    persist(id, node).map { maybeUpdated =>
      for {
        updated <- maybeUpdated
        nodeId <- updated.id
        envReq <- node.environmentRequirement
      } yield {
        logger.info(s"Saving updated environment requirement data for node $nodeId")
        saveEnvReq(nodeId, envReq)
      }
      maybeUpdated
    }.map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!
   */
  def addStorageUnit(storageUnit: StorageUnit)(implicit currUsr: String): Future[StorageUnit] = {
    addStorageNode(storageUnit)(storageUnitDao.insert)
  }

  /**
   * TODO: Document me!!!
   */
  def addRoom(storageRoom: Room)(implicit currUsr: String): Future[Room] = {
    addStorageNode(storageRoom)(roomDao.insert)
  }

  /**
   * TODO: Document me!!!
   */
  def addBuilding(building: Building)(implicit currUsr: String): Future[Building] = {
    addStorageNode(building)(buildingDao.insert)
  }

  /**
   * TODO: Document me!!!
   */
  def addOrganisation(organisation: Organisation)(implicit currUsr: String): Future[Organisation] = {
    addStorageNode(organisation)(organisationDao.insert)
  }

  /**
   * TODO: Document me!
   */
  def updateStorageUnit(
    id: StorageNodeId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[MusitResult[Option[StorageUnit]]] = {
    updateStorageNode(id, storageUnit)(storageUnitDao.update)
  }

  /**
   * TODO: Document me!!!
   */
  def updateRoom(
    id: StorageNodeId,
    room: Room
  )(implicit currUsr: String): Future[MusitResult[Option[Room]]] = {
    updateStorageNode(id, room)(roomDao.update)
  }

  /**
   * TODO: Document me!!!
   */
  def updateBuilding(
    id: StorageNodeId,
    building: Building
  )(implicit currUsr: String): Future[MusitResult[Option[Building]]] = {
    updateStorageNode(id, building)(buildingDao.update)
  }

  /**
   * TODO: Document me!!!
   */
  def updateOrganisation(
    id: StorageNodeId,
    organisation: Organisation
  )(implicit currUsr: String): Future[MusitResult[Option[Organisation]]] = {
    updateStorageNode(id, organisation)(organisationDao.update)
  }

  /**
   * TODO: Document me!
   */
  def getStorageUnitById(
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] =
    storageUnitDao.getById(id).map(maybeRes => MusitSuccess(maybeRes))

  /**
   * TODO: Document me!!!
   */
  def getRoomById(id: StorageNodeId): Future[MusitResult[Option[Room]]] = {
    roomDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(id: StorageNodeId): Future[MusitResult[Option[Building]]] = {
    buildingDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def getOrganisationById(id: StorageNodeId): Future[MusitResult[Option[Organisation]]] = {
    organisationDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!
   */
  def getNodeById(
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageNode]]] = {
    storageUnitDao.getNodeById(id).flatMap { maybeNode =>
      maybeNode.map { node =>
        node.storageType match {
          case StorageType.StorageUnitType =>
            Future.successful {
              MusitSuccess(Option(StorageNodeDto.toStorageUnit(node)))
            }

          case StorageType.BuildingType =>
            getBuildingById(id)

          case StorageType.RoomType =>
            getRoomById(id)

          case StorageType.OrganisationType =>
            getOrganisationById(id)

        }
      }.getOrElse(Future.successful(MusitSuccess[Option[StorageNode]](None)))
    }

  }

  /**
   * TODO: Document me!
   */
  def getChildren(id: StorageNodeId): Future[Seq[StorageNode]] = {
    storageUnitDao.getChildren(id).map(_.map(StorageNodeDto.toStorageNode))
  }

  /**
   * TODO: Document me!
   */
  def getStorageType(id: StorageNodeId): Future[MusitResult[Option[StorageType]]] = {
    storageUnitDao.getStorageType(id)
  }

  /**
   * TODO: Document me!
   */
  def verifyStorageTypeMatchesDatabase(
    id: StorageNodeId,
    expected: StorageType
  ): Future[Boolean] = {
    getStorageType(id).map {
      case MusitSuccess(correct) if Option(expected) == correct =>
        true

      case MusitSuccess(wrong) =>
        logger.warn(
          s"StorageUnit with id: $id was expected to have storage " +
            s"type: ${expected.entryName}, but had the " +
            s"type: ${wrong.map(_.entryName).getOrElse("NA")} in the database."
        )
        false

      case err: MusitError[_] =>
        false
    }
  }

  //  def updateStorageTripleById(
  //    id: StorageNodeId,
  //    triple: StorageNode
  //  ): Future[MusitResult[Int]] = {
  //    val sid = StorageNodeId(id)
  //    verifyStorageTypeMatchesDatabase(sid, triple.storageType).flatMap {
  //      case Right(true) =>
  //        triple match {
  //          case st: StorageUnit =>
  //            updateStorageUnitById(sid, st).map(Right(_))
  //          case building: Building =>
  //            buildingService.updateBuilding(sid, building).map(Right(_))
  //          case room: Room =>
  //            roomService.update(sid, room).map(Right(_))
  //        }
  //      case Left(error) =>
  //        Future.successful(Left(error))
  //    }
  //  }

  /**
   * TODO: Document me!
   */
  def deleteNode(id: StorageNodeId)(implicit currUsr: String): Future[MusitResult[Int]] = {
    storageUnitDao.nodeExists(id).flatMap {
      case MusitSuccess(exists) =>
        if (exists) storageUnitDao.markAsDeleted(id)
        else Future.successful(MusitSuccess(0))

      case error =>
        Future.successful(error.asInstanceOf[MusitError[Int]])
    }
  }
}
