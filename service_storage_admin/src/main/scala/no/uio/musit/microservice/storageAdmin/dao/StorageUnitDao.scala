package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Storage, StorageUnit }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import no.uio.musit.microservices.common.extensions.OptionExtensions.OptionExtensionsImp

/** * Handles the storageNode table. */
@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val envReqDao: EnvReqDao
) extends HasDatabaseConfigProvider[JdbcProfile] with StorageDtoConverter {

  import driver.api._

  implicit lazy val storageTypeMapper = MappedColumnType.base[StorageType, String](
    storageType => storageType.toString,
    string => StorageType.fromString(string)
  )

  private val StorageNodeTable = TableQuery[StorageNodeTable]

  def unknownStorageUnitMsg(id: Long) = s"Unknown storageUnit with id: $id"

  def storageUnitNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(unknownStorageUnitMsg(id))

  def getStorageNodeOnlyById(id: Long): Future[Option[StorageNodeDTO]] =
    db.run(StorageNodeTable.filter(st => st.id === id && st.isDeleted === false).result.headOption)

  def getChildren(id: Long): Future[Seq[StorageNodeDTO]] = {
    val action = StorageNodeTable.filter(st => st.isPartOf === id && st.isDeleted === false).result
    db.run(action)
  }

  def getStorageType(id: Long): MusitFuture[StorageType] = {
    db.run(StorageNodeTable.filter(st => st.id === id && st.isDeleted === false).map(_.storageType).result.headOption)
      .foldInnerOption(Left(storageUnitNotFoundError(id)), Right(_))
  }

  def all(): Future[Seq[StorageNodeDTO]] =
    db.run(StorageNodeTable.filter(st => st.isDeleted === false).result)

  def rootNodes(readGroup: String): Future[Seq[StorageNodeDTO]] =
    db.run(StorageNodeTable.filter(st => st.isDeleted === false && st.isPartOf.isEmpty && st.groupRead === readGroup).result)

  def setPartOf(id: Long, partOf: Long): Future[Int] =
    db.run(StorageNodeTable.filter(_.id === id).map(_.isPartOf).update(Some(partOf)))

  def insert(storageUnit: StorageUnit): Future[StorageNodeDTO] =
    insert(storageUnitToDto(storageUnit))

  def insert(storageUnit: CompleteStorageUnitDto): Future[StorageNodeDTO] =
    db.run(insertAction(storageUnit.storageNode))

  def insertStorageUnit(completeStorageUnitDto: CompleteStorageUnitDto): Future[CompleteStorageUnitDto] = {
    val envReqInsertAction = envReqDao.insertAction(completeStorageUnitDto.envReqDto)
    val nodePartIn = completeStorageUnitDto.storageNode

    val action = (for {
      optEnvReq <- envReqInsertAction
      nodePart = nodePartIn.copy(latestEnvReqId = optEnvReq.map(_.id).flatten)
      nodePartOut <- insertAction(nodePart)
    } yield CompleteStorageUnitDto(nodePartOut, optEnvReq)).transactionally
    db.run(action)
  }

  def insertStorageUnit(storageUnit: StorageUnit): Future[StorageUnit] = {
    insertStorageUnit(storageUnitToDto(storageUnit)).map(storageUnitFromDto)
  }

  def insertAction(storageNodePart: StorageNodeDTO): DBIO[StorageNodeDTO] = {
    StorageNodeTable returning StorageNodeTable.map(_.id) into
      ((storageNode, id) =>
        storageNode.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageNodePart
  }

  /* def getLatestEnvReqId(id: Long) : Future[Option[Long]] = {
     db.run(StorageNodeTable.filter(st => st.id === id && st.isDeleted === false).map(_.latestEnvReqId)
       .result.headOption).map(_.flatten)
   }*/

  def updateStorageNodeAction(id: Long, storageNodeDto: StorageNodeDTO): DBIO[Int] = {
    StorageNodeTable.filter(st => st.id === id && st.isDeleted === false).update(storageNodeDto)
  }

  //An action updating the common node part and also updates/creates a new envReq if different from the current one
  def updateStorageNodeAndMaybeEnvReqAction(id: Long, storage: Storage) /*: Future[DBIO[Int]]*/ = {
    def insertEnvReqAndUpdateNode(combinedNodeDto: StorageNodeDTO, envReqDto: EnvReqDto) = {
      (for {
        envReq <- envReqDao.insertAction(envReqDto)
        nodePart = combinedNodeDto.copy(latestEnvReqId = envReq.id)
        n <- updateStorageNodeAction(id, nodePart) //update storageNode, med sine data og siste envHid
      } yield n).transactionally
    }
    DBIO.from(getStorageNodeOnlyById(id)).flatMap { optNodeInDatabase =>
      val nodeInDatabase = optNodeInDatabase.getOrFail(s"Unable to find storage node with id: $id")
      val newStorageNodeDto = toDto(storage)
      require(nodeInDatabase.id == Some(id))
      val combinedNodeDto = newStorageNodeDto.storageNode.copy(latestMoveId = nodeInDatabase.latestMoveId,
                                        latestEnvReqId = nodeInDatabase.latestEnvReqId,id = nodeInDatabase.id)
      val envReqAction = DBIO.successful[Option[EnvReqDto]](None) //TEMP!
      assert(newStorageNodeDto.envReqDto.isDefined == storage.environmentRequirement.isDefined)

      nodeInDatabase.latestEnvReqId match {
        case None => {
          newStorageNodeDto.envReqDto match {
            case None => updateStorageNodeAction(id, combinedNodeDto) //No latestEnvReqId and no current EnvReq, so only update node
            //No latestEnvReqId, but we have data for a new one
            case Some(envReqDto) => insertEnvReqAndUpdateNode(combinedNodeDto, envReqDto)
          }
        }
        case Some(latestEnvReqId) => {
          //Get it from the database and compare it to the new one. If the new one is different, then create the new one.
          newStorageNodeDto.envReqDto match {
            case None => //Have latestEnvReqId and no current EnvReq, so we need to insert a new "blank" EnvReq
              insertEnvReqAndUpdateNode(combinedNodeDto, EnvReqDto.createBlank)
            //Have latestEnvReqId, and we have data for a new one
            case Some(envReqDto) => {
              val futOptOldEnvReqDto = envReqDao.getById(latestEnvReqId)
              DBIO.from(futOptOldEnvReqDto).flatMap { optOldEnvReqDto =>
                val oldEnvReqDto = optOldEnvReqDto.getOrFail(s"Unable to find existing EnvReq with id: $latestEnvReqId")
                val oldEndReqDomain = fromEnvReqDto(oldEnvReqDto)
                val newEnvReqDomain = storage.environmentRequirement.getOrFail("should have an envReq here!")
                if (oldEndReqDomain == newEnvReqDomain) {
                  //We do the equality check on the domain class, because we
                  // don't want to compare against reqistered date and other potential "hidden" fields in the dto class.
                  updateStorageNodeAction(id, combinedNodeDto)
                } else {
                  insertEnvReqAndUpdateNode(combinedNodeDto, envReqDto)
                }
              }
            }
          }
        }
      }
    }
  }

  def updateStorageUnitAndMaybeEnvReq(id: Long, storageUnit: Storage): Future[Int] = {
    db.run(updateStorageNodeAndMaybeEnvReqAction(id, storageUnit))
  }

  def deleteStorageNode(id: Long): Future[Int] = {
    db.run((for {
      storageUnit <- StorageNodeTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  private class StorageNodeTable(tag: Tag) extends Table[StorageNodeDTO](tag, Some("MUSARK_STORAGE"), "STORAGE_NODE") {
    def * = (id.?, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, latestMoveId, latestEnvReqId,
      isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[StorageType]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_NODE_NAME")

    val area = column[Option[Double]]("AREA")

    val areaTo = column[Option[Double]]("AREA_TO")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Double]]("HEIGHT")

    val heightTo = column[Option[Double]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val latestMoveId = column[Option[Long]]("LATEST_MOVE_ID")

    val isDeleted = column[Boolean]("IS_DELETED")

    val latestEnvReqId = column[Option[Long]]("LATEST_ENVREQ_ID")

    def create = (
      id: Option[Long],
      storageType: StorageType,
      storageUnitName: String,
      area: Option[Double],
      areaTo: Option[Double],
      isPartOf: Option[Long],
      height: Option[Double],
      heightTo: Option[Double],
      groupRead: Option[String],
      groupWrite: Option[String],
      latestMoveId: Option[Long],
      latestEnvReqId: Option[Long],
      isDeleted: Boolean
    ) =>
      StorageNodeDTO(
        id,
        storageUnitName,
        area,
        areaTo,
        isPartOf,
        height,
        heightTo,
        groupRead,
        groupWrite,
        latestMoveId,
        latestEnvReqId,
        Storage.linkText(id),
        isDeleted,
        storageType
      )

    def destroy(unit: StorageNodeDTO) =
      Some(
        unit.id,
        unit.storageType,
        unit.name,
        unit.area,
        unit.areaTo,
        unit.isPartOf,
        unit.height,
        unit.heightTo,
        unit.groupRead,
        unit.groupWrite,
        unit.latestMoveId,
        unit.latestEnvReqId,
        unit.isDeleted
      )
  }

}
