package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.Note
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext

@Singleton
class NoteDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao,
    override val eventDocumentDao: EventDocumentDao
) extends ConservationEventDao[Note] {

  override val logger = Logger(classOf[NoteDao])
}
