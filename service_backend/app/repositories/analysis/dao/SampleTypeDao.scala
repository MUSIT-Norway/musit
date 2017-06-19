package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.SampleType
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.DbErrorHandlers

import scala.concurrent.Future

@Singleton
class SampleTypeDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
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
