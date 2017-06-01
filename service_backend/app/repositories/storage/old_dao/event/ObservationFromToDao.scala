package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.ObservationFromToDto
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
@Singleton
class ObservationFromToDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  val logger = Logger(classOf[ObservationFromToDao])

  /**
   * TODO: Document me!
   */
  def insertAction(event: ObservationFromToDto): DBIO[Int] = {
    logger.debug(s"Received ObservationFromTo with parentId ${event.id}")
    obsFromToTable += event
  }

  /**
   * TODO: Document me!
   */
  def getObservationFromTo(id: EventId): Future[Option[ObservationFromToDto]] =
    db.run(obsFromToTable.filter(event => event.id === id).result.headOption)

}
