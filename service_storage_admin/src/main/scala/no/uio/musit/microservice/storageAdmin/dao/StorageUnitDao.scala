package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain.dto.{ StorageNodeDTO, StorageType }
import no.uio.musit.microservice.storageAdmin.domain.Storage
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import slick.jdbc.SQLActionBuilder
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  implicit lazy val storageTypeMapper = MappedColumnType.base[StorageType, String](
    storageType => storageType.toString,
    string => StorageType.fromString(string)
  )

  private val StorageUnitTable = TableQuery[StorageUnitTable]

  def unknownStorageUnitMsg(id: Long) = s"Unknown storageUnit with id: $id"

  def storageUnitNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(unknownStorageUnitMsg(id))

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageNodeDTO]] =
    db.run(StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).result.headOption)

  def getChildren(id: Long): Future[Seq[StorageNodeDTO]] = {
    val action = StorageUnitTable.filter(st => st.isPartOf === id && st.isDeleted === false).result
    db.run(action)
  }

  def getPath(id: Long): Future[Seq[StorageNodeDTO]] = {
    val optSelf = getStorageUnitOnlyById(id)
    optSelf.flatMap {
      case None => Future.successful(Seq.empty)
      case Some(self) =>
        self.isPartOf match {
          case None => Future.successful(Seq(self))
          case Some(parentId) =>
            val futOptParent = getStorageUnitOnlyById(parentId)
            futOptParent.flatMap {
              case None => Future.successful(Seq(self))
              case Some(parent) =>
                val futParentPath = getPath(parent.id.get)
                futParentPath.map { parentPath =>
                  parentPath :+ self
                }
            }
        }
    }
  }

  def getStorageType(id: Long): MusitFuture[StorageType] = {
    db.run(StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).map(_.storageType).result.headOption)
      .foldInnerOption(Left(storageUnitNotFoundError(id)), Right(_))
  }

  def all(): Future[Seq[StorageNodeDTO]] =
    db.run(StorageUnitTable.filter(st => st.isDeleted === false).result)

  def rootNodes(readGroup: String): Future[Seq[StorageNodeDTO]] =
    db.run(StorageUnitTable.filter(st => st.isDeleted === false && st.isPartOf.isEmpty && st.groupRead === readGroup).result)

  def setPartOf(id: Long, partOf: Long): Future[Int] =
    db.run(StorageUnitTable.filter(_.id === id).map(_.isPartOf).update(Some(partOf)))

  def insert(storageUnit: StorageNodeDTO): Future[StorageNodeDTO] =
    db.run(insertAction(storageUnit))

  def insertAction(storageUnit: StorageNodeDTO): DBIO[StorageNodeDTO] = {
    StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) =>
        storageUnit.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageUnit
  }

  def updateStorageUnitAction(id: Long, storageUnit: StorageNodeDTO): DBIO[Int] = {
    StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  def updateStorageUnit(id: Long, storageUnit: StorageNodeDTO): Future[Int] = {
    db.run(updateStorageUnitAction(id, storageUnit))
  }

  def deleteStorageUnit(id: Long): Future[Int] = {
    db.run((for {
      storageUnit <- StorageUnitTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  private class StorageUnitTable(tag: Tag) extends Table[StorageNodeDTO](tag, Some("MUSARK_STORAGE"), "STORAGE_NODE") {
    def * = (id.?, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, latestMoveId, latestEnvReqId,
      isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[StorageType]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_NODE_NAME")

    val area = column[Option[Long]]("AREA")

    val areaTo = column[Option[Long]]("AREA_TO")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val heightTo = column[Option[Long]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val latestMoveId = column[Option[Long]]("LATEST_MOVE_ID")

    val isDeleted = column[Boolean]("IS_DELETED")

    val latestEnvReqId = column[Option[Long]]("LATEST_ENVREQ_ID")

    def create = (
      id: Option[Long],
      storageType: StorageType,
      storageUnitName: String,
      area: Option[Long],
      areaTo: Option[Long],
      isPartOf: Option[Long],
      height: Option[Long],
      heightTo: Option[Long],
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
