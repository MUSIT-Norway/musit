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
    //At the moment no filtering on user or collection is done, so we can reuse allEventTypes
    allEventTypes()
  }

  /*All event types, irrespective of user. Used by the system itself.  */
  def allEventTypes(): FutureMusitResult[Seq[ConservationType]] = {
    val res = daoUtils.dbRun(
      conservationTypeTable.result,
      s"A problem occurred fetching conservation types from the DB"
    )
    res.map(ctr => ctr.map(fromConservationTypeRow))
  }
}
