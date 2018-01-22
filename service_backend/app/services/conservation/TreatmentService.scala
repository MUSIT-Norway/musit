package services.conservation

import com.google.inject.Inject
import models.conservation.events.Treatment
import models.conservation.{TreatmentKeyword, TreatmentMaterial}
import no.uio.musit.functional.FutureMusitResult
import repositories.conservation.dao.TreatmentDao

import scala.concurrent.ExecutionContext

class TreatmentService @Inject()(
    implicit
    override val dao: TreatmentDao,
    val consService: ConservationService,
    override val ec: ExecutionContext
) extends ConservationEventService[Treatment] {

  def getMaterialList: FutureMusitResult[Seq[TreatmentMaterial]] = {
    dao.getMaterialList
  }
  def getKeywordList: FutureMusitResult[Seq[TreatmentKeyword]] = {
    dao.getKeywordList
  }
}
