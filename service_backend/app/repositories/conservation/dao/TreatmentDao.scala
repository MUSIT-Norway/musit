package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.Treatment
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

@Singleton
class TreatmentDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext
) extends ConservationEventDao[Treatment] {

  override val logger = Logger(classOf[TreatmentDao])

}
