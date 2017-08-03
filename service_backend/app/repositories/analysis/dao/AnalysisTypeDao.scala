package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.{AnalysisType, AnalysisTypeId, Category}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.CollectionUUID
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class AnalysisTypeDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[AnalysisTypeDao])

  import profile.api._

  /**
   * Returns all analysis types from the database.
   */
  def all: Future[MusitResult[Seq[AnalysisType]]] = {
    db.run(analysisTypeTable.result)
      .map { res =>
        val ats = res.map(fromAnalysisTypeRow)
        MusitSuccess(ats)
      }
      .recover(nonFatal("A problem occurred fetching all analysis types from the DB"))
  }

  def allFor(
      maybeCat: Option[Category],
      maybeColl: Option[CollectionUUID]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[AnalysisType]]] = {
    val catQuery = maybeCat
      .map(cat => analysisTypeTable.filter(_.category === cat))
      .getOrElse(analysisTypeTable)
    val collQuery = {
      if (currUser.hasGodMode) {
        maybeColl
          .map(coll => {
            catQuery.filter { at =>
              at.collections.isEmpty || (at.collections like s"%,${coll.asString},%")
            }
          })
          .getOrElse(catQuery)
      } else {
        catQuery
      }
    }
    db.run(collQuery.result)
      .map { res =>
        val ats = res.map(fromAnalysisTypeRow)
        MusitSuccess(ats)
      }
      .recover(
        nonFatal(
          s"A problem occurred fetching analysis types for category " +
            s"${maybeCat.map(_.entryName).getOrElse("N/A")} and collection " +
            s"${maybeColl.map(_.toString).getOrElse("N/A")} from the DB"
        )
      )
  }

  /**
   * Returns all analysis types within the given Category.
   */
  def allForCategory(c: Category): Future[MusitResult[Seq[AnalysisType]]] = {
    db.run(analysisTypeTable.filter(_.category === c).result)
      .map { res =>
        val ats = res.map(fromAnalysisTypeRow)
        MusitSuccess(ats)
      }
      .recover(
        nonFatal(
          s"A problem occurred fetching analysis types for category " +
            s"${c.entryName} from the DB"
        )
      )
  }

  /**
   * Will return the anlysis type associated by the {{{AnalysisTypeId}}}. Or
   * None if the ID can't be found.
   */
  def findById(tid: AnalysisTypeId): Future[MusitResult[Option[AnalysisType]]] = {
    db.run(analysisTypeTable.filter(_.typeId === tid).result.headOption)
      .map(r => MusitSuccess(r.map(fromAnalysisTypeRow)))
      .recover(
        nonFatal(s"A problem occurred fetching analysis type by id $tid from the DB")
      )
  }

  /**
   * Returns all analysis' for the specified CollectionUUID. Including the ones
   * that have no specific collections configured.
   */
  def allForCollection(cid: CollectionUUID): Future[MusitResult[Seq[AnalysisType]]] = {
    val q = analysisTypeTable.filter { at =>
      at.collections.isEmpty || (at.collections like s"%,${cid.asString},%")
    }

    db.run(q.result)
      .map { res =>
        val ats = res.map(fromAnalysisTypeRow)
        MusitSuccess(ats)
      }
      .recover(
        nonFatal(
          s"A problem occurred fetching analysis types for collection $cid from the DB"
        )
      )
  }

}
