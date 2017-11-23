package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.StorageAndHandling
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext

@Singleton
class StorageAndHandlingDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao
) extends ConservationEventDao[StorageAndHandling] {

  override val logger = Logger(classOf[StorageAndHandlingDao])

}
