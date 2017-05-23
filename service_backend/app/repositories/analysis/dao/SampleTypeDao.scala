package repositories.analysis.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.SampleType
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class SampleTypeDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends AnalysisTables {

  val logger = Logger(classOf[SampleTypeDao])

  import profile.api._

  def getSampleTypeList: Future[MusitResult[Seq[SampleType]]] = {
    db.run(sampleTypeTable.result)
      .map(_.map(fromSampleTypeRow))
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(ex) =>
          val msg = s"An unexpected error occurred fetching sample Type list"
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }
}
