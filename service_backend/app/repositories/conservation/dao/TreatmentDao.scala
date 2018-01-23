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
    override val actorRoleDao: ActorRoleDateDao,
    override val eventDocumentDao: EventDocumentDao
) extends ConservationEventDao[Treatment]
    with ConservationEventTableProvider {

  override val logger = Logger(classOf[TreatmentDao])

  import profile.api._

  private def getMaterialBothNoAndEn(
      material: TreatmentMaterial
  ): TreatmentMaterial = {
    TreatmentMaterial(
      material.id,
      material.noTerm,
      if (material.enTerm.isEmpty)
        Some(material.noTerm + "[NO]")
      else material.enTerm
    )
  }

  def getMaterialList: FutureMusitResult[Seq[TreatmentMaterial]] = {
    val action =
      treatmentMaterialTable.result.map(
        seq =>
          seq.map(
            m => getMaterialBothNoAndEn(m)
        )
      )
    daoUtils.dbRun(action, "getMaterialList failed")
  }

  private def getKeywordBothNoAndEn(
      keyword: TreatmentKeyword
  ): TreatmentKeyword = {
    TreatmentKeyword(
      keyword.id,
      keyword.noTerm,
      if (keyword.enTerm.isEmpty)
        Some(keyword.noTerm + "[NO]")
      else keyword.enTerm
    )
  }

  def getKeywordList: FutureMusitResult[Seq[TreatmentKeyword]] = {
    val action =
      treatmentKeywordTable.result.map(
        seq =>
          seq.map(
            m => getKeywordBothNoAndEn(m)
        )
      )
    daoUtils.dbRun(action, "something went wrong in getKeywordList")
    // daoUtils.dbRun(treatmentKeywordTable.result, "something went wrong in getKeywordList")
  }

  def getEventRowFromEventTable(eventId: EventId): FutureMusitResult[EventRow] = {
    val q = eventTable.filter(_.eventId === eventId).result.headOption
    daoUtils
      .dbRun(q, "something went wrong in getEventRowFromEventTable")
      .getOrError(MusitValidationError("didn't find record"))
  }
}
