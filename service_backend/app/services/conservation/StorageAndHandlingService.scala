package services.conservation

import com.google.inject.Inject
import models.conservation.events.StorageAndHandling
import repositories.conservation.dao.StorageAndHandlingDao

import scala.concurrent.ExecutionContext

class StorageAndHandlingService @Inject()(
    implicit
    override val dao: StorageAndHandlingDao,
    val consService: ConservationService,
    override val ec: ExecutionContext
) extends ConservationEventService[StorageAndHandling] {}
