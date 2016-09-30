package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{ MusitDbError, MusitResult, MusitSuccess }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class KdReportDao @Inject() (val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def getReportTotalArea: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node
        WHERE storage_type = 'Room'
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }

  def getAreaPerimeterSecurity: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node s, musark_storage.room r
        WHERE s.storage_node_id = r.storage_node_id
        AND r.perimeter_security = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaPerimeterSecurity", Some(e))
      }
  }

  def getAreaTheftProtection: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node s, musark_storage.room r
        WHERE s.storage_node_id = r.storage_node_id
        AND r.theft_protection = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaTheftProtection", Some(e))
      }
  }

  def getAreaFireProtectiony: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node s, musark_storage.room r
        WHERE s.storage_node_id = r.storage_node_id
        AND r.fire_protection = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaFireProtection", Some(e))
      }
  }

  def getAreaWaterDamageAssessment: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node s, musark_storage.room r
        WHERE s.storage_node_id = r.storage_node_id
        AND r.water_damage_assessment = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaWaterDamageAssessment", Some(e))
      }
  }

  def getAreaRoutinesAndContingencyPlan: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM(area_to)
        FROM musark_storage.storage_node s, musark_storage.room r
        WHERE  s.storage_node_id = r.storage_node_id
        AND r.routines_and_contingency_plan = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaRoutinesAndContingencyPlan", Some(e))
      }
  }

}

