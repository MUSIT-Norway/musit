package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.ConservationType
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.CollectionUUID
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class ConservationTypeDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils
) extends ConservationTables
    with DbErrorHandlers {

  val logger = Logger(classOf[ConservationTypeDao])

  import profile.api._

  /**
   * Returns all conservation types from the database.
   */
  def allFor(
      maybeColl: Option[CollectionUUID]
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[Seq[ConservationType]] = {
    val query = conservationTypeTable
    val collQuery = {
      if (currUser.hasGodMode) {
        maybeColl
          .map(coll => {
            query.filter { at =>
              at.collections.isEmpty || (at.collections like s"%,${coll.asString},%")
            }
          })
          .getOrElse(query)
      } else {
        query
      }
    }
    val res = daoUtils.dbRun(
      collQuery.result,
      s"A problem occurred fetching conservation types for collection $maybeColl from the DB"
    )
    res.map(ctr => ctr.map(fromConservationTypeRow))
  }
}
