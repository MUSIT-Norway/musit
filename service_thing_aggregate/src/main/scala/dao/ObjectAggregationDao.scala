package dao

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{ MusitDbError, MusitResult, MusitSuccess }
import models.{ MuseumId, MuseumIdentifier, ObjectAggregation, ObjectId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def getObjects(mid: MuseumId, nodeId: Long): Future[MusitResult[Seq[ObjectAggregation]]] = {
    implicit val getObject = GetResult(r =>
      ObjectAggregation(ObjectId(r.nextLong), MuseumIdentifier.fromSqlString(r.nextString), r.nextStringOption))
    db.run(
      sql"""
         SELECT id, displayid, displayname
         FROM  musark_storage.local_object, musark_mapping.view_musitthing
         WHERE museumId = ${mid.underlying} and current_location_id = $nodeId
         AND object_id = Id
      """.as[ObjectAggregation].map(MusitSuccess.apply)
    ).recover {
        case e: Exception => MusitDbError("Error occurred while retrieving objects", Some(e))
      }
  }
}
