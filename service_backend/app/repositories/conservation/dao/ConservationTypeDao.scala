package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.ConservationType
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.CollectionUUID
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class ConservationTypeDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ConservationTables
    with DbErrorHandlers {

  val logger = Logger(classOf[ConservationTypeDao])

  import profile.api._

  /**
   * Returns all conservation types from the database.
   */
  def allFor(
      maybeColl: Option[CollectionUUID]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[ConservationType]]] = {
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

    db.run(collQuery.result)
      .map { res =>
        val ats = res.map(fromConservationTypeRow)
        MusitSuccess(ats)
      }
      .recover(
        nonFatal(
          s"A problem occurred fetching conservation types for collection $maybeColl from the DB"
        )
      )
  }

}
