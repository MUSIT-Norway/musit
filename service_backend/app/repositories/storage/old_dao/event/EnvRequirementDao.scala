package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.EnvRequirementDto
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
@Singleton
class EnvRequirementDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  /**
   * TODO: Document me!
   */
  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    envReqTable += event

  /**
   * TODO: Document me!
   */
  def getEnvRequirement(id: EventId): Future[Option[EnvRequirementDto]] =
    db.run(envReqTable.filter(event => event.id === id).result.headOption)

}
