package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storageAdmin.domain.dto.{CompleteStorageUnitDto, StorageDtoConverter, StorageNodeDTO, StorageType}
import no.uio.musit.microservice.storageAdmin.domain.{Storage, StorageUnit}
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

/*** Handles the storageNode table. */
@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
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

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageNodeDTO]] =
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

  def insertAction(storageNodePart: StorageNodeDTO): DBIO[StorageNodeDTO] = {
    StorageNodeTable returning StorageNodeTable.map(_.id) into
      ((storageNode, id) =>
        storageNode.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageNodePart
  }

  def updateNodeUnitAction(id: Long, storageUnit: StorageNodeDTO): DBIO[Int] = {
    StorageNodeTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  def updateStorageNode(id: Long, storageNode: StorageNodeDTO): Future[Int] = {
    db.run(updateNodeUnitAction(id, storageNode))
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
