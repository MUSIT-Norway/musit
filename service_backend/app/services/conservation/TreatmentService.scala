package services.conservation

import com.google.inject.Inject
import models.conservation.events.Treatment
import repositories.conservation.dao.TreatmentDao

import scala.concurrent.ExecutionContext

class TreatmentService @Inject()(
    implicit
    override val dao: TreatmentDao,
    override val ec: ExecutionContext
) extends ConservationEventService[Treatment] {}
