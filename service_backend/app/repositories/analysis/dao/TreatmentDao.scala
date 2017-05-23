package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.Treatment
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class TreatmentDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables {

  val logger = Logger(classOf[TreatmentDao])

  import profile.api._

  def getTreatmentList: Future[MusitResult[Seq[Treatment]]] = {
    db.run(treatmentTable.result)
      .map(_.map(fromTreatmentRow))
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(ex) =>
          val msg = s"An unexpected error occurred fetching treatment list"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }
}
