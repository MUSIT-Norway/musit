package services.conservation

import com.google.inject.Inject
import models.conservation.{TreatmentKeyword, TreatmentMaterial}
import models.conservation.events.Treatment
import no.uio.musit.MusitResults.MusitResult
import repositories.conservation.dao.TreatmentDao

import scala.concurrent.{ExecutionContext, Future}

class TreatmentService @Inject()(
    implicit
    override val dao: TreatmentDao,
    override val ec: ExecutionContext
) extends ConservationEventService[Treatment] {

  def getMaterialList: Future[MusitResult[Seq[TreatmentMaterial]]] = {
    dao.getMaterialList
  }
  def getKeywordList: Future[MusitResult[Seq[TreatmentKeyword]]] = {
    dao.getKeywordList
  }
}
