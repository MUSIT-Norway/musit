package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservice.storageAdmin.domain.{ Storage, StorageType, StorageUnit }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.json.Json
import slick.driver.JdbcProfile

import scala.concurrent.Future

object StorageUnitDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  implicit lazy val storageTypeMapper = MappedColumnType.base[StorageType, String](
    storageType => storageType.entryName,
    string => StorageType.withName(string)
  )

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val StorageUnitTable = TableQuery[StorageUnitTable]

  def unknownStorageUnitMsg(id: Long) = s"Unknown storageUnit with id: $id"

  def storageUnitNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(unknownStorageUnitMsg(id))

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageUnitDTO]] =
    db.run(StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).result.headOption)

  def getChildren(id: Long): Future[Seq[StorageUnitDTO]] = {
    val action = StorageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  def getStorageType(id: Long): MusitFuture[StorageType] = {
    db.run(StorageUnitTable.filter(_.id === id).map(_.storageType).result.headOption)
      .foldInnerOption(Left(storageUnitNotFoundError(id)), Right(_))
  }

  def all(): Future[Seq[StorageUnitDTO]] =
    db.run(StorageUnitTable.filter(st => st.isDeleted === false).result)

  def rootNodes(readGroup: String): Future[Seq[StorageUnitDTO]] =
    db.run(StorageUnitTable.filter(st => st.isDeleted === false && st.isPartOf.isEmpty && st.groupRead === readGroup).result)

  def insert(storageUnit: StorageUnitDTO): Future[StorageUnitDTO] =
    db.run(insertAction(storageUnit))

  def insertAction(storageUnit: StorageUnitDTO): DBIO[StorageUnitDTO] = {
    StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) =>
        storageUnit.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageUnit
  }

  def updateStorageUnitAction(id: Long, storageUnit: StorageUnitDTO): DBIO[Int] = {
    StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  def updateStorageUnit(id: Long, storageUnit: StorageUnitDTO): Future[Int] = {
    db.run(updateStorageUnitAction(id, storageUnit))
  }

  def deleteStorageUnit(id: Long): Future[Int] = {
    db.run((for {
      storageUnit <- StorageUnitTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnitDTO](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id.?, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[StorageType]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_UNIT_NAME")

    val area = column[Option[Long]]("AREA")

    val areaTo = column[Option[Long]]("AREA_TO")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val heightTo = column[Option[Long]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val isDeleted = column[Boolean]("IS_DELETED")

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
      isDeleted: Boolean
    ) =>
      StorageUnitDTO(
        id,
        storageUnitName,
        area,
        areaTo,
        isPartOf,
        height,
        heightTo,
        groupRead,
        groupWrite,
        Storage.linkText(id),
        Option(isDeleted),
        storageType
      )

    def destroy(unit: StorageUnitDTO) =
      Some(
        unit.id,
        unit.`type`,
        unit.name,
        unit.area,
        unit.areaTo,
        unit.isPartOf,
        unit.height,
        unit.heightTo,
        unit.groupRead,
        unit.groupWrite,
        unit.isDeleted.getOrElse(false)
      )
  }

}
