package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.KdReportDao
import no.uio.musit.microservice.storagefacility.domain.report.KdReport
import no.uio.musit.models.MuseumId
import no.uio.musit.service.MusitResults.MusitResult

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * TODO: Document me!!!
 */
class KdReportService @Inject() (val kdReportDao: KdReportDao) {

  private def flatMapResult(
    totalRes: MusitResult[Double],
    perimeterRes: MusitResult[Double],
    theftRes: MusitResult[Double],
    fireRes: MusitResult[Double],
    waterRes: MusitResult[Double],
    contingencyRes: MusitResult[Double]
  ): MusitResult[KdReport] = {
    for {
      total <- totalRes
      perimeter <- perimeterRes
      theft <- theftRes
      fire <- fireRes
      water <- waterRes
      contingency <- contingencyRes
    } yield KdReport(
      totalArea = total,
      perimeterSecurity = perimeter,
      theftProtection = theft,
      fireProtection = fire,
      waterDamageAssessment = water,
      routinesAndContingencyPlan = contingency
    )
  }

  def getReport(mid: MuseumId): Future[MusitResult[KdReport]] = {
    val futTotArea = kdReportDao.getReportTotalArea(mid)
    val futAreaPerimeter = kdReportDao.getAreaPerimeterSecurity(mid)
    val futAreaTheft = kdReportDao.getAreaTheftProtection(mid)
    val futAreaFire = kdReportDao.getAreaFireProtectiony(mid)
    val futAreaWaterDamage = kdReportDao.getAreaWaterDamageAssessment(mid)
    val futAreaContingency = kdReportDao.getAreaRoutinesAndContingencyPlan(mid)
    for {
      areaRes <- futTotArea
      perimeterRes <- futAreaPerimeter
      theftRes <- futAreaTheft
      fireRes <- futAreaFire
      waterRes <- futAreaWaterDamage
      contingencyRes <- futAreaContingency
    } yield {
      flatMapResult(
        totalRes = areaRes,
        perimeterRes = perimeterRes,
        theftRes = theftRes,
        fireRes = fireRes,
        waterRes = waterRes,
        contingencyRes = contingencyRes
      )
    }
  }

}
