package services.conservation

import com.google.inject.Inject
import models.conservation.events.Note
import repositories.conservation.dao.NoteDao

import scala.concurrent.ExecutionContext

class NoteService @Inject()(
    implicit
    override val dao: NoteDao,
    override val ec: ExecutionContext
) extends ConservationEventService[Note] {}
