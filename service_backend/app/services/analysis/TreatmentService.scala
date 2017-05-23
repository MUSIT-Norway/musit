package services.analysis

import com.google.inject.Inject
import models.analysis.Treatment
import no.uio.musit.MusitResults.MusitResult
import play.api.Logger
import repositories.analysis.dao.TreatmentDao

import scala.concurrent.Future

class TreatmentService @Inject()(
    val treatDao: TreatmentDao
) {

  val logger = Logger(classOf[TreatmentService])

  def getTreatmentList: Future[MusitResult[Seq[Treatment]]] = {
    treatDao.getTreatmentList
  }
}
