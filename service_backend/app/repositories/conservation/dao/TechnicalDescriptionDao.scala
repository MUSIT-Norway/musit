package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.TechnicalDescription
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext

@Singleton
class TechnicalDescriptionDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao,
    override val eventDocumentDao: EventDocumentDao
) extends ConservationEventDao[TechnicalDescription] {

  override val logger = Logger(classOf[TechnicalDescriptionDao])

}
