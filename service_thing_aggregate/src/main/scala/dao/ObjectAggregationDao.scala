package dao

import com.google.inject.Inject
import models.MusitResults.{ MusitDbError, MusitResult, MusitSuccess }
import models.{ MuseumIdentifier, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
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
         select id, displayId, displayName
         from MUSARK_STORAGE.LOCAL_OBJECT, MUSIT_MAPPING.VIEW_MUSITTHING
         WHERE current_location_id = $nodeId and object_id = id;
      """.as[ObjectAggregation].map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }
}
