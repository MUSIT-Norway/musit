package dao

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import models.MuseumId
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

class StorageNodeDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def nodeExists(mid: MuseumId, nodeId: Long): Future[MusitResult[Boolean]] = {
    db.run(
      sql"""
         select count(*)
         from "MUSARK_STORAGE"."STORAGE_NODE"
         where "MUSEUM_ID" = ${mid.underlying}
         and "STORAGE_NODE_ID" = $nodeId
      """.as[Long].head.map(res => MusitSuccess(res == 1))
    ).recover {
        case NonFatal(e) =>
          MusitDbError(s"Error occurred while checking for node existence for nodeId $nodeId", Some(e))
      }
  }
}
