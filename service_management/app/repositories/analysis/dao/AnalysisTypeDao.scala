package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.{AnalysisType, Category}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.CollectionUUID
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AnalysisTypeDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[AnalysisTypeDao])

  import driver.api._

  /**
   * Returns all analysis types from the database.
   */
  def all: Future[MusitResult[Seq[AnalysisType]]] = {
    db.run(analysisTypeTable.result)
      .map { res =>
        val ats = res.map(fromAnalysisTypeRow)
        MusitSuccess(ats)
      }
      .recover {
        case NonFatal(ex) =>
          val msg = "A problem occurred fetching all analysis types from the DB"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
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
      .recover {
        case NonFatal(ex) =>
          val msg = s"A problem occurred fetching analysis types for category " +
            s"${c.entryName} from the DB"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
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
      .recover {
        case NonFatal(ex) =>
          val msg = s"A problem occurred fetching analysis types for collection " +
            s"$cid from the DB"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

}
