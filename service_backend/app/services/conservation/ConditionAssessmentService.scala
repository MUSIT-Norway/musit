package services.conservation

import com.google.inject.Inject
import models.conservation.ConditionCode
import models.conservation.events.ConditionAssessment
import no.uio.musit.functional.FutureMusitResult
import repositories.conservation.dao.ConditionAssessmentDao

import scala.concurrent.ExecutionContext

class ConditionAssessmentService @Inject()(
    implicit
    override val dao: ConditionAssessmentDao,
    override val ec: ExecutionContext
) extends ConservationEventService[ConditionAssessment] {

  def getConditionCodeList: FutureMusitResult[Seq[ConditionCode]] = {
    dao.getConditionCodeList
  }
}
