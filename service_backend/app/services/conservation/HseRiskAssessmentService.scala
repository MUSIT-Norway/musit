package services.conservation

import com.google.inject.Inject
import models.conservation.events.HseRiskAssessment
import repositories.conservation.dao.HseRiskAssessmentDao

import scala.concurrent.ExecutionContext

class HseRiskAssessmentService @Inject()(
    implicit
    override val dao: HseRiskAssessmentDao,
    val consService: ConservationService,
    override val ec: ExecutionContext
) extends ConservationEventService[HseRiskAssessment] {}
