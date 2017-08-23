package services.reporting

import com.google.inject.Inject
import models.reporting.KdReport
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.MuseumId
import repositories.reporting.dao.KdReportDao

import scala.concurrent.{ExecutionContext, Future}

/**
 * TODO: Document me!!!
 */
class KdReportService @Inject()(
    implicit
    val dao: KdReportDao,
    val ec: ExecutionContext
) {

  def getReport(mid: MuseumId): Future[MusitResult[KdReport]] = {
    val totArea         = MusitResultT(dao.getReportTotalArea(mid))
    val areaPerimeter   = MusitResultT(dao.getAreaPerimeterSecurity(mid))
    val areaTheft       = MusitResultT(dao.getAreaTheftProtection(mid))
    val areaFire        = MusitResultT(dao.getAreaFireProtectiony(mid))
    val areaWaterDamage = MusitResultT(dao.getAreaWaterDamageAssessment(mid))
    val areaContingency = MusitResultT(dao.getAreaRoutinesAndContingencyPlan(mid))

    val report = for {
      area        <- totArea
      perimeter   <- areaPerimeter
      theft       <- areaTheft
      fire        <- areaFire
      water       <- areaWaterDamage
      contingency <- areaContingency
    } yield {
      KdReport(
        totalArea = area,
        perimeterSecurity = perimeter,
        theftProtection = theft,
        fireProtection = fire,
        waterDamageAssessment = water,
        routinesAndContingencyPlan = contingency
      )
    }

    report.value
  }

}
