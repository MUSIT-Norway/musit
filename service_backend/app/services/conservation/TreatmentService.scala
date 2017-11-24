package services.conservation

import com.google.inject.Inject
import models.conservation.{TreatmentKeyword, TreatmentMaterial}
import models.conservation.events.Treatment
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.FutureMusitResult
import repositories.conservation.dao.TreatmentDao

import scala.concurrent.{ExecutionContext, Future}

class TreatmentService @Inject()(
    implicit
    override val dao: TreatmentDao,
    override val ec: ExecutionContext
) extends ConservationEventService[Treatment] {

  def getMaterialList: FutureMusitResult[Seq[TreatmentMaterial]] = {
    dao.getMaterialList
  }
  def getKeywordList: FutureMusitResult[Seq[TreatmentKeyword]] = {
    dao.getKeywordList
  }
}
