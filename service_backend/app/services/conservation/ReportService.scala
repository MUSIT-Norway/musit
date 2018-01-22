package services.conservation

import com.google.inject.Inject
import models.conservation.events.Report
import repositories.conservation.dao.ReportDao

import scala.concurrent.ExecutionContext

class ReportService @Inject()(
    implicit
    override val dao: ReportDao,
    val consService: ConservationService,
    override val ec: ExecutionContext
) extends ConservationEventService[Report] {}
