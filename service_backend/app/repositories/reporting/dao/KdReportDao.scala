package repositories.reporting.dao

import com.google.inject.Inject
import models.storage.nodes.StorageType
import models.storage.nodes.StorageType.RoomType
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.MuseumId
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.StorageTables

import scala.concurrent.{ExecutionContext, Future}

class KdReportDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends StorageTables {

  import profile.api._

  private val roomType: StorageType = RoomType

  def getReportTotalArea(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = storageNodeTable.filter { sn =>
      sn.storageType === roomType && sn.museumId === mid && sn.isDeleted === false
    }.map(_.area)

    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(nonFatal("Error occurred while retrieving objects"))
  }

  def getAreaPerimeterSecurity(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.perimeterSecurity === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(nonFatal("Error occurred while retrieving areaPerimeterSecurity"))
  }

  def getAreaTheftProtection(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.theftProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(nonFatal("Error occurred while retrieving AreaTheftProtection"))
  }

  def getAreaFireProtectiony(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.fireProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(nonFatal("Error occurred while retrieving AreaFireProtection"))
  }

  def getAreaWaterDamageAssessment(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.waterDamage === true)
    } yield {
      sn.area
    }
    val action = query.sum.result
    db.run(action)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(nonFatal("Error occurred while retrieving areaWaterDamageAssessment"))
  }

  def getAreaRoutinesAndContingencyPlan(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.routinesAndContingency === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result)
      .map(res => MusitSuccess(res.getOrElse(0.0)))
      .recover(
        nonFatal("Error occurred while retrieving AreaRoutinesAndContingencyPlan")
      )
  }

}
