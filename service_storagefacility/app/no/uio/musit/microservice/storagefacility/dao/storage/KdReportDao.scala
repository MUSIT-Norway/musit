package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.domain.storage.StorageType
import no.uio.musit.microservice.storagefacility.domain.storage.StorageType.RoomType
import no.uio.musit.models.MuseumId
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

class KdReportDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  private val roomType: StorageType = RoomType

  def getReportTotalArea(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = storageNodeTable.filter { sn =>
      sn.storageType === roomType && sn.museumId === mid && sn.isDeleted === false
    }.map(_.area)

    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }

  def getAreaPerimeterSecurity(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r <- roomTable.filter(r => sn.id === r.id && r.perimeterSecurity === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving areaPerimeterSecurity", Some(e))
      }
  }

  def getAreaTheftProtection(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r <- roomTable.filter(r => sn.id === r.id && r.theftProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving AreaTheftProtection", Some(e))
      }
  }

  def getAreaFireProtectiony(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r <- roomTable.filter(r => sn.id === r.id && r.fireProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving AreaFireProtection", Some(e))
      }
  }

  def getAreaWaterDamageAssessment(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r <- roomTable.filter(r => sn.id === r.id && r.waterDamage === true)
    } yield {
      sn.area
    }
    val action = query.sum.result
    db.run(action)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving areaWaterDamageAssessment", Some(e))
      }
  }

  def getAreaRoutinesAndContingencyPlan(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r <- roomTable.filter(r => sn.id === r.id && r.routinesAndContingency === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover {
        case NonFatal(e) =>
          MusitDbError("Error occurred while retrieving AreaRoutinesAndContingencyPlan", Some(e))
      }
  }

}

