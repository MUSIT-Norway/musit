package services.conservation

import com.google.inject.Inject
import models.conservation.events.{MeasurementData, MeasurementDetermination}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{MuseumId, ObjectUUID}
import repositories.conservation.dao.MeasurementDeterminationDao

import scala.concurrent.ExecutionContext

class MeasurementDeterminationService @Inject()(
    implicit
    override val dao: MeasurementDeterminationDao,
    val consService: ConservationService,
    override val ec: ExecutionContext
) extends ConservationEventService[MeasurementDetermination] {

  def getCurrentMeasurement(
      mid: MuseumId,
      oUuid: ObjectUUID
  ): FutureMusitResult[Option[MeasurementData]] = {
    val res =
      dao.getCurrentEventForSpecificEventType(oUuid, MeasurementDetermination.eventTypeId)

    res.flatMap {
      case Some(m) => dao.getMeasurementData(m)
      case None    => FutureMusitResult.successful(None)
    }
  }

}
