package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class RoomDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao,
    val envReqDao: EnvReqDao
) extends HasDatabaseConfigProvider[JdbcProfile] with StorageDtoConverter {

  import driver.api._

  private val RoomTable = TableQuery[RoomTable]

  def getRoomById(id: Long): Future[Option[RoomDTO]] = {
    val action = RoomTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  private def updateRoomOnlyAction(id: Long, storageRoom: RoomDTO): DBIO[Int] = {
    RoomTable.filter(_.id === id).update(storageRoom)
  }

  /** Returns how many rooms has been updated. (1 or else error) */
  def updateRoom(id: Long, room: Room): Future[Int] = {
    val roomDto = roomToDto(room)
    val storageNodePart = roomDto.storageNode
    val roomPart = roomDto.roomDto

    //If we don't have the storage unit or it is marked as deleted, or we find more than 1 rows to update, onlyAcceptOneUpdatedRecord
    // will make this DBIO/Future fail with an appropriate MusitException.
    // (Which later gets recovered in ServiceHelper.daoUpdate)
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageNodeAndMaybeEnvReqAction(id, room)
    //TODO: Pipe the above into DaoHelper.onlyAcceptOneUpdatedRecord or similar (like it was done in an earlier version of this code),
    // because the above line needs to throw an exception if it for some reason doesn't update the given row
    //(As an example, we don't want to update the RoomOnly if the node has been logically deleted or the user doesn't have write access to the node in the first place)

    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateRoomOnlyAction(id, roomPart.copy(id = Some(id))) }

    db.run(combinedAction.transactionally)
  }

  private def insertRoomOnlyAction(storageRoom: RoomDTO): DBIO[Int] = {
    require(storageRoom.id.isDefined) //if failed then it's our bug
    val insertQuery = RoomTable
    val action = insertQuery += storageRoom
    action
  }

  def insertRoom(completeRoomDto: CompleteRoomDto): Future[CompleteRoomDto] = {
    val envReqInsertAction = envReqDao.insertAction(completeRoomDto.envReqDto)

    val nodePartIn = completeRoomDto.storageNode
    val roomPartIn = completeRoomDto.roomDto

    val action = (for {
      optEnvReq <- envReqInsertAction
      nodePart = nodePartIn.copy(latestEnvReqId = optEnvReq.map(_.id).flatten)
      nodePartOut <- storageUnitDao.insertAction(nodePart)
      roomPartOut = roomPartIn.copy(id = nodePartOut.id)
      n <- insertRoomOnlyAction(roomPartOut)
    } yield CompleteRoomDto(nodePartOut, roomPartOut, optEnvReq)).transactionally
    db.run(action)
  }

  def insertRoom(room: Room): Future[Room] = {
    insertRoom(roomToDto(room)).map(roomFromDto)
  }

  private class RoomTable(tag: Tag) extends Table[RoomDTO](tag, Some("MUSARK_STORAGE"), "ROOM") {

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
      RoomDTO(
        id,
        perimeterSecurity,
        theftProtection,
        fireProtection,
        waterDamageAssessment,
        routinesAndContingencyPlan,
        relativeHumidity,
        temperatureAssessment,
        lightingCondition,
        preventiveConservation
      )

    def destroy(room: RoomDTO) = Some(room.id, room.perimeterSecurity, room.theftProtection,
      room.fireProtection, room.waterDamageAssessment,
      room.routinesAndContingencyPlan, room.relativeHumidity, room.temperatureAssessment, room.lightingCondition, room.preventiveConservation)
  }

}
