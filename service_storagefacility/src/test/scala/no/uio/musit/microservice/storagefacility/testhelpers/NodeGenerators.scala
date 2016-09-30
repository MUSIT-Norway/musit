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

package no.uio.musit.microservice.storagefacility.testhelpers

import no.uio.musit.microservice.storagefacility.dao.storage.{ BuildingDao, OrganisationDao, RoomDao, StorageUnitDao }
import no.uio.musit.microservice.storagefacility.domain.{ Interval, NodePath }
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.test.MusitSpecWithApp
import play.api.Application

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

trait NodeGenerators extends NodeTypeInitializers {
  self: MusitSpecWithApp =>

  def buildingDao: BuildingDao = {
    val instance = Application.instanceCache[BuildingDao]
    instance(musitFakeApp)
  }

  def organisationDao: OrganisationDao = {
    val instance = Application.instanceCache[OrganisationDao]
    instance(musitFakeApp)
  }

  def roomDao: RoomDao = {
    val instance = Application.instanceCache[RoomDao]
    instance(musitFakeApp)
  }

  def storageUnitDao: StorageUnitDao = {
    val instance = Application.instanceCache[StorageUnitDao]
    instance(musitFakeApp)
  }

  // Some default nodes
  lazy val defaultBuilding: Building = {
    Await.result(buildingDao.insert(createBuilding()), 5 seconds)
  }

  lazy val defaultRoom: Room = {
    Await.result(roomDao.insert(createRoom()), 5 seconds)
  }

  lazy val defaultStorageUnit: StorageUnit = {
    Await.result(storageUnitDao.insert(createStorageUnit()), 5 seconds)
  }

  def addRoot(r: Root) = storageUnitDao.insertRoot(r)

  def addBuilding(b: Building) = buildingDao.insert(b)

  def addOrganisation(o: Organisation) = organisationDao.insert(o)

  def addRoom(r: Room) = roomDao.insert(r)

  def addStorageUnit(su: StorageUnit) = storageUnitDao.insert(su)

  def addNode(nodes: StorageNode*): Seq[StorageNodeId] = {
    val eventuallyInserted = Future.sequence {
      nodes.filterNot(_.isInstanceOf[GenericStorageNode]).map {
        case su: StorageUnit => addStorageUnit(su)
        case r: Room => addRoom(r)
        case b: Building => addBuilding(b)
        case o: Organisation => addOrganisation(o)
        case r: Root => addRoot(r)
        case notCorrect =>
          throw new IllegalArgumentException( // scalastyle:ignore
            s"${notCorrect.getClass} is not supported"
          )
      }
    }.map { inserted =>
      inserted.map(_.id.get)
    }
    Await.result(eventuallyInserted, 10 seconds)
  }
}

trait NodeTypeInitializers {

  def initEnvironmentRequirement(
    temp: Option[Interval[Double]] = Some(Interval[Double](20.0, Some(25))),
    humid: Option[Interval[Double]] = Some(Interval[Double](60.7, Some(70))),
    hypoxic: Option[Interval[Double]] = Some(Interval[Double](12.0, Some(20))),
    cleaning: Option[String] = Some("Keep it clean!"),
    lighting: Option[String] = Some("Dempet belysning"),
    comment: Option[String] = Some("Kommentar for environment requirement.")
  ): EnvironmentRequirement = {
    EnvironmentRequirement(
      temperature = temp,
      relativeHumidity = humid,
      hypoxicAir = hypoxic,
      cleaning = cleaning,
      lightingCondition = lighting,
      comment = comment
    )
  }

  val defaultEnvironmentRequirement: EnvironmentRequirement =
    initEnvironmentRequirement()

  def createBuilding(
    name: String = "FooBarBuilding",
    partOf: Option[StorageNodeId] = None,
    path: Option[NodePath] = None
  ): Building = {
    Building(
      id = None,
      name = name,
      area = Some(200),
      areaTo = Some(250),
      isPartOf = partOf,
      height = Some(5),
      heightTo = Some(8),
      groupRead = None,
      groupWrite = None,
      path = path,
      environmentRequirement = Some(defaultEnvironmentRequirement),
      address = Some("FooBar Gate 8, 111 Oslo, Norge")
    )
  }

  def createRoom(
    name: String = "FooRoom",
    partOf: Option[StorageNodeId] = None,
    path: Option[NodePath] = None
  ): Room = {
    Room(
      id = None,
      name = name,
      area = Some(50),
      areaTo = Some(55),
      height = Some(2),
      heightTo = Some(3),
      isPartOf = partOf,
      groupRead = None,
      groupWrite = None,
      path = path,
      environmentRequirement = Some(defaultEnvironmentRequirement),
      securityAssessment = SecurityAssessment(
        perimeter = Some(true),
        theftProtection = Some(true),
        fireProtection = Some(true),
        waterDamage = Some(false),
        routinesAndContingencyPlan = Some(false)
      ),
      environmentAssessment = EnvironmentAssessment(
        relativeHumidity = Some(true),
        lightingCondition = Some(true),
        temperature = Some(true),
        preventiveConservation = Some(false)
      )
    )
  }

  def createStorageUnit(
    name: String = "FooUnit",
    partOf: Option[StorageNodeId] = None,
    path: Option[NodePath] = None
  ): StorageUnit = {
    StorageUnit(
      id = None,
      name = "FooUnit",
      area = Some(1),
      areaTo = Some(2),
      isPartOf = partOf,
      height = Some(2),
      heightTo = Some(2),
      groupRead = None,
      groupWrite = None,
      path = path,
      environmentRequirement = Some(defaultEnvironmentRequirement)
    )
  }

  def createRoomWithDifferentArea(
    areaTo: Double,
    perimeter: Boolean = false,
    theftProtection: Boolean = false,
    fireProtection: Boolean = false,
    waterDamage: Boolean = false,
    routinesAndContingencyPlan: Boolean = false
  ): Room = {
    createRoom().copy(
      id = None,
      name = "MyPrivateRoom",
      areaTo = Some(areaTo),
      environmentRequirement = Some(defaultEnvironmentRequirement),
      securityAssessment = SecurityAssessment(
        perimeter = Some(perimeter),
        theftProtection = Some(theftProtection),
        fireProtection = Some(fireProtection),
        waterDamage = Some(waterDamage),
        routinesAndContingencyPlan = Some(routinesAndContingencyPlan)
      )
    )
  }

}
