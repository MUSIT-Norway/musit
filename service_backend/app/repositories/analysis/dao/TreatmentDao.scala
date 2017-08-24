package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.Treatment
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TreatmentDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[TreatmentDao])

  import profile.api._

  def getTreatmentList: Future[MusitResult[Seq[Treatment]]] = {
    db.run(treatmentTable.result)
      .map(_.map(fromTreatmentRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching treatment list"))
  }
}
