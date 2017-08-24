package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.SampleType
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SampleTypeDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends AnalysisTables
    with DbErrorHandlers {

  val logger = Logger(classOf[SampleTypeDao])

  import profile.api._

  def getSampleTypeList: Future[MusitResult[Seq[SampleType]]] = {
    db.run(sampleTypeTable.result)
      .map(_.map(fromSampleTypeRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching sample Type list"))
  }
}
