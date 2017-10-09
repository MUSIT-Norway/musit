package services.conservation

import com.google.inject.Inject
import models.conservation.events.TechnicalDescription
import repositories.conservation.dao.TechnicalDescriptionDao

import scala.concurrent.ExecutionContext

class TechnicalDescriptionService @Inject()(
    implicit
    override val dao: TechnicalDescriptionDao,
    override val ec: ExecutionContext
) extends ConservationEventService[TechnicalDescription] {}
