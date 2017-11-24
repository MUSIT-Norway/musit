package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.{TreatmentKeyword, TreatmentMaterial}
import models.conservation.events.Treatment
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.EventId
import no.uio.musit.repositories.events.BaseEventTableProvider
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils
import no.uio.musit.functional.Extensions._

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class TreatmentDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao
) extends ConservationEventDao[Treatment]
    with ConservationEventTableProvider {

  override val logger = Logger(classOf[TreatmentDao])

  import profile.api._

  def getMaterialList: FutureMusitResult[Seq[TreatmentMaterial]] = {
    daoUtils
      .dbRun(treatmentMaterialTable.result, "something went wrong in getMaterialList")
  }

  def getKeywordList: FutureMusitResult[Seq[TreatmentKeyword]] = {
    daoUtils.dbRun(treatmentKeywordTable.result, "something went wrong in getKeywordList")
  }

  def getEventRowFromEventTable(eventId: EventId): FutureMusitResult[EventRow] = {
    val q = eventTable.filter(_.eventId === eventId).result.headOption
    daoUtils
      .dbRun(q, "something went wrong in getEventRowFromEventTable")
      .getOrError(MusitValidationError("didn't find record"))
  }
}
