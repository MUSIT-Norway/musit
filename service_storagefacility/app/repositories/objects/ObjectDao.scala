package repositories.objects

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{ObjectId, ObjectUUID}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.shared.dao.DbErrorHandlers
import slick.jdbc.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * This DAO is needed to be able to fulfill the move objects calls from the
 * Delphi clients.
 */
class ObjectDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with DbErrorHandlers {

  import profile.api._

  def uuidsForIds(
      ids: Seq[ObjectId]
  ): Future[MusitResult[Seq[(ObjectId, ObjectUUID)]]] = {
    val query =
      sql"""
           SELECT t.OBJECT_ID, t.MUSITTHING_UUID FROM MUSIT_MAPPING.MUSITTHING t
           WHERE t.OBJECT_ID IN (#${ids.mkString(",")})
         """.as[(Long, String)]

    db.run(query)
      .map { res =>
        MusitSuccess(
          res.map(r => ObjectId.fromLong(r._1) -> ObjectUUID.unsafeFromString(r._2))
        )
      }
      .recover(nonFatal("An error occurred trying to fetch UUIDs for objects"))
  }

}
