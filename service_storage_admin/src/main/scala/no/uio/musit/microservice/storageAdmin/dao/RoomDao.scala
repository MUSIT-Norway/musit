package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class RoomDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val RoomTable = TableQuery[RoomTable]

  def getRoomById(id: Long): Future[Option[Room]] = {
    val action = RoomTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  private def updateRoomOnlyAction(id: Long, storageRoom: Room): DBIO[Int] = {
    RoomTable.filter(_.id === id).update(storageRoom)
  }

  def updateRoom(id: Long, room: Room) = {

    //If we don't have the storage unit or it is marked as deleted, or we find more than 1 rows to update, onlyAcceptOneUpdatedRecord
    // will make this DBIO/Future fail with an appropriate MusitException.
    // (Which later gets recovered in ServiceHelper.daoUpdate)
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageUnitAction(id, Storage.toDTO(room))

    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateRoomOnlyAction(id, room.copy(id = Some(id))) }

    db.run(combinedAction.transactionally)
  }

  private def insertRoomOnlyAction(storageRoom: Room): DBIO[Int] = {
    assert(storageRoom.id.isDefined) //if failed then it's our bug
    val stRoom = storageRoom.copy(links = Storage.linkText(storageRoom.id))
    val insertQuery = RoomTable
    val action = insertQuery += stRoom
    action
  }

  def insertRoom(storageUnit: StorageNodeDTO, storageRoom: Room): Future[Storage] = {
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(storageUnit)
      n <- insertRoomOnlyAction(storageRoom.copy(id = storageUnit.id))
    } yield Storage.getRoom(storageUnit, storageRoom)).transactionally
    db.run(action)
  }

  private class RoomTable(tag: Tag) extends Table[Room](tag, Some("MUSARK_STORAGE"), "ROOM") {

    def * = (id, perimeterSecurity, theftProtection, fireProtection, waterDamageAssessment, // scalastyle:ignore
      routinesAndContingencyPlan, relativeHumidity, temperatureAssessment, lightingCondition, preventiveConservation) <> (create.tupled, destroy)

    def id = column[Option[Long]]("STORAGE_NODE_ID", O.PrimaryKey)

    def perimeterSecurity = column[Option[Boolean]]("PERIMETER_SECURITY")

    def theftProtection = column[Option[Boolean]]("THEFT_PROTECTION")

    def fireProtection = column[Option[Boolean]]("FIRE_PROTECTION")

    def waterDamageAssessment = column[Option[Boolean]]("WATER_DAMAGE_ASSESSMENT")

    def routinesAndContingencyPlan = column[Option[Boolean]]("ROUTINES_AND_CONTINGENCY_PLAN")

    def relativeHumidity = column[Option[Boolean]]("RELATIVE_HUMIDITY")
    def temperatureAssessment = column[Option[Boolean]]("TEMPERATURE_ASSESSMENT")

    def lightingCondition = column[Option[Boolean]]("LIGHTING_CONDITION")

    def preventiveConservation = column[Option[Boolean]]("PREVENTIVE_CONSERVATION")

    def create = (
      id: Option[Long], perimeterSecurity: Option[Boolean], theftProtection: Option[Boolean],
      fireProtection: Option[Boolean], waterDamageAssessment: Option[Boolean],
      routinesAndContingencyPlan: Option[Boolean], relativeHumidity: Option[Boolean], temperatureAssessment: Option[Boolean],
      lightingCondition: Option[Boolean], preventiveConservation: Option[Boolean]
    ) =>
      Room(id, null, None, None, None, None, None, None, None, None, None, None,
        perimeterSecurity,
        theftProtection,
        fireProtection,
        waterDamageAssessment,
        routinesAndContingencyPlan,
        relativeHumidity,
        temperatureAssessment,
        lightingCondition,
        preventiveConservation)

    def destroy(room: Room) = Some(room.id, room.perimeterSecurity, room.theftProtection,
      room.fireProtection, room.waterDamageAssessment,
      room.routinesAndContingencyPlan, room.relativeHumidity, room.temperatureAssessment, room.lightingCondition, room.preventiveConservation)
  }

}
