package dao

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import models._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  val logger = Logger(classOf[ObjectAggregationDao])

  import driver.api._

  def getObjects(mid: MuseumId, nodeId: Long): Future[MusitResult[Seq[ObjectAggregation]]] = {
    implicit val getObject = GetResult(r =>
      ObjectAggregation(
        id = ObjectId(r.nextLong),
        museumNo = MuseumNo(r.nextString),
        subNo = r.nextStringOption.map(SubNo.apply),
        term = r.nextStringOption
      ))
    db.run(
      sql"""
         SELECT "MUSITTHING"."ID", "MUSITTHING"."MUSEUMNO", "MUSITTHING"."SUBNO", "MUSITTHING"."TERM"
         FROM "MUSARK_STORAGE"."LOCAL_OBJECT", "MUSIT_MAPPING"."MUSITTHING"
         WHERE "LOCAL_OBJECT"."MUSEUM_ID" = ${mid.underlying}
         AND "LOCAL_OBJECT"."CURRENT_LOCATION_ID" = ${nodeId}
         AND "LOCAL_OBJECT"."OBJECT_ID" = "ID";
      """.as[ObjectAggregation].map(MusitSuccess.apply)
    ).recover {
        case e: Exception =>
          val msg = s"Error while retrieving objects for nodeId $nodeId"
          logger.error(msg, e)
          MusitDbError(msg, Some(e))
      }
  }
}
