package dao

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import models.{MuseumIdentifier, ObjectAggregation, ObjectId}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def getObjects(nodeId: Long): Future[MusitResult[Seq[ObjectAggregation]]] = {
    implicit val getObject = GetResult(r =>
      ObjectAggregation(ObjectId(r.nextLong), MuseumIdentifier.fromSqlString(r.nextString), r.nextStringOption))
    db.run(
      sql"""
         select "ID", "DISPLAYID", "DISPLAYNAME"
         from "MUSARK_STORAGE"."LOCAL_OBJECT", "MUSIT_MAPPING"."VIEW_MUSITTHING"
         WHERE "CURRENT_LOCATION_ID" = $nodeId and "OBJECT_ID" = "ID";
      """.as[ObjectAggregation].map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }
}
