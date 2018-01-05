package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.{MeasurementData, MeasurementDetermination}
import no.uio.musit.MusitResults
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext

@Singleton
class MeasurementDeterminationDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao,
    override val daoUtils: DaoUtils,
    override val actorRoleDao: ActorRoleDateDao,
    override val eventDocumentDao: EventDocumentDao
) extends ConservationEventDao[MeasurementDetermination] {

  override val logger = Logger(classOf[MeasurementDeterminationDao])

  import profile.api._

  import no.uio.musit.functional.Extensions._;
  def getMeasurementData(eId: EventId): FutureMusitResult[Option[MeasurementData]] = {
    val action =
      eventTable.filter(e => e.eventId === eId).map(m => m.eventJson).result.headOption

    val msdata = daoUtils
      .dbRun(
        action,
        s"An unexpected error occurred fetching objects in getSpecialAttributes for event $eId"
      )
      .flatMapInsideOption { jsv =>
        val jsMmd = (jsv \ "measurementData")
        FutureMusitResult.from(
          MusitResults.MusitResult.fromJsResult(jsMmd.validate[MeasurementData])
        )
      }

    msdata
  }

}
