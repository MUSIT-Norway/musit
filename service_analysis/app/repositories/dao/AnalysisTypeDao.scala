package repositories.dao

import com.google.inject.{Inject, Singleton}
import models.events.{AnalysisType, Category}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AnalysisTypeDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[AnalysisTypeDao])

  import driver.api._

  def all: Future[MusitResult[Seq[AnalysisType]]] = {
    db.run(analysisTypeTable.result).map { res =>
      val ats = res.map(fromAnalysisTypeRow)
      MusitSuccess(ats)
    }.recover {
      case NonFatal(ex) =>
        val msg = "A problem occurred fetching all analysis types from the DB"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def allForCategory(c: Category): Future[MusitResult[Seq[AnalysisType]]] = {
    db.run(analysisTypeTable.filter(_.category === c).result).map { res =>
      val ats = res.map(fromAnalysisTypeRow)
      MusitSuccess(ats)
    }.recover {
      case NonFatal(ex) =>
        val msg = s"A problem occurred fetching analysis types for category " +
          s"${c.entryName} from the DB"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
