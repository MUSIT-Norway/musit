package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.ConditionCode
import models.conservation.events.ConditionAssessment
import no.uio.musit.functional.FutureMusitResult
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext
@Singleton
class ConditionAssessmentDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao
) extends ConservationEventDao[ConditionAssessment]
    with ConservationEventTableProvider {

  override val logger = Logger(classOf[ConditionAssessmentDao])

  import profile.api._

  def getConditionCodeList: FutureMusitResult[Seq[ConditionCode]] = {
    daoUtils.dbRun(conditionCode.result, "something went wrong in getConditionCodeList")
  }
}
