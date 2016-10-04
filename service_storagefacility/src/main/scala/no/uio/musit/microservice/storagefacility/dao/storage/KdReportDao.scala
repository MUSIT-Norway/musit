package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class KdReportDao @Inject() (val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def getReportTotalArea: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE"
        WHERE "STORAGE_TYPE" = 'Room'
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }

  def getAreaPerimeterSecurity: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE", "MUSARK_STORAGE"."ROOM"
        WHERE "STORAGE_NODE"."STORAGE_NODE_ID" = "ROOM"."STORAGE_NODE_ID"
        AND "ROOM"."PERIMETER_SECURITY" = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaPerimeterSecurity", Some(e))
      }
  }

  def getAreaTheftProtection: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE", "MUSARK_STORAGE"."ROOM"
        WHERE "STORAGE_NODE"."STORAGE_NODE_ID" = "ROOM"."STORAGE_NODE_ID"
        AND "ROOM"."THEFT_PROTECTION" = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaTheftProtection", Some(e))
      }
  }

  def getAreaFireProtectiony: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE", "MUSARK_STORAGE"."ROOM"
        WHERE "STORAGE_NODE"."STORAGE_NODE_ID" = "ROOM"."STORAGE_NODE_ID"
        AND "ROOM"."FIRE_PROTECTION" = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaFireProtection", Some(e))
      }
  }

  def getAreaWaterDamageAssessment: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE", "MUSARK_STORAGE"."ROOM"
        WHERE "STORAGE_NODE"."STORAGE_NODE_ID" = "ROOM"."STORAGE_NODE_ID"
        AND "ROOM"."WATER_DAMAGE_ASSESSMENT" = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving areaWaterDamageAssessment", Some(e))
      }
  }

  def getAreaRoutinesAndContingencyPlan: Future[MusitResult[Double]] = {
    db.run(
      sql"""
        SELECT SUM("STORAGE_NODE"."AREA_TO")
        FROM "MUSARK_STORAGE"."STORAGE_NODE", "MUSARK_STORAGE"."ROOM"
        WHERE "STORAGE_NODE"."STORAGE_NODE_ID" = "ROOM"."STORAGE_NODE_ID"
        AND "ROOM"."ROUTINES_AND_CONTINGENCY_PLAN" = 1
      """.as[Double].head.map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving AreaRoutinesAndContingencyPlan", Some(e))
      }
  }

}

