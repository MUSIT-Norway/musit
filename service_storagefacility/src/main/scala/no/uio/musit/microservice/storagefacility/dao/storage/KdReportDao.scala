package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{ MusitDbError, MusitResult, MusitSuccess }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by ellenjo on 26.09.16.
 */

class KdReportDao @Inject() (val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def getReportTotalArea: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE
         WHERE storage_type = 'Room'
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }

  /*
perimeterSecurity: Int,
theftProtection: Int,
fireProtection: Int,
waterDamageAssessment : Int,
routinesAndContingencyPlan: Int

fire_protection INTEGER, -- DEFAULT 0 NOT NULL,
  water_damage_assessment INTEGER, -- DEFAULT 0 NOT NULL,
  routines_and_contingency_plan INTEGER, -*/

  def getAreaPerimeterSecurity: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE s, MUSARK_STORAGE.ROOM r
         WHERE s.storage_node_id = r.storage_node_id
         and r.perimeter_security = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaPerimeterSecurity", Some(e))
      }
  }

  def getAreaTheftProtection: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE s, MUSARK_STORAGE.ROOM r
         WHERE s.storage_node_id = r.storage_node_id
         and r.theft_protection = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaTheftProtection", Some(e))
      }
  }

  def getAreaFireProtectiony: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE s, MUSARK_STORAGE.ROOM r
         WHERE s.storage_node_id = r.storage_node_id
         and r.fire_protection = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaFireProtection", Some(e))
      }
  }

  def getAreaWaterDamageAssessment: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE s, MUSARK_STORAGE.ROOM r
         WHERE s.storage_node_id = r.storage_node_id
         and r.water_damage_assessment = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaWaterDamageAssessment", Some(e))
      }
  }

  def getAreaRoutinesAndContingencyPlan: Future[MusitResult[Double]] = {
    db.run(
      sql"""
         select SUM(area_to)
         from MUSARK_STORAGE.STORAGE_NODE s, MUSARK_STORAGE.ROOM r
         WHERE  s.storage_node_id = r.storage_node_id
         and r.routines_and_contingency_plan = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaRoutinesAndContingencyPlan", Some(e))
      }
  }

}

