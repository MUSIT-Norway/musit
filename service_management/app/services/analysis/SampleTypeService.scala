package services.analysis

import com.google.inject.Inject
import models.analysis.SampleType
import no.uio.musit.MusitResults.MusitResult
import play.api.Logger
import repositories.analysis.dao.SampleTypeDao

import scala.concurrent.Future

class SampleTypeService @Inject()(
    val sampleTypeDao: SampleTypeDao
) {

  val logger = Logger(classOf[SampleTypeService])

  def getSampleTypeList: Future[MusitResult[Seq[SampleType]]] = {
    sampleTypeDao.getSampleTypeList
  }
}
