package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.KdReportDao
import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.report.KdReport
import no.uio.musit.service.MusitResults.MusitResult

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * TODO: Document me!!!
 */
class KdReportService @Inject() (val kdReportDao: KdReportDao) {
  def getReport(mid: MuseumId): Future[MusitResult[KdReport]] = {
    val futTotArea = kdReportDao.getReportTotalArea(mid)
    val futAreaPerimeterSecurity = kdReportDao.getAreaPerimeterSecurity(mid)
    val futAreaTheftProtection = kdReportDao.getAreaTheftProtection(mid)
    val futAreaFireProtection = kdReportDao.getAreaFireProtectiony(mid)
    val futAreaWaterDamageAssessment = kdReportDao.getAreaWaterDamageAssessment(mid)
    val futAreaRoutinesAndContingencyPlan = kdReportDao.getAreaRoutinesAndContingencyPlan(mid)
    for {
      mrTotArea <- futTotArea
      mrAreaPerimeterSecurity <- futAreaPerimeterSecurity
      mrAreaTheftProtection <- futAreaTheftProtection
      mrAreaFireProtection <- futAreaFireProtection
      mrAreaWaterDamageAssessment <- futAreaWaterDamageAssessment
      mrAreaRoutinesAndContingencyPlan <- futAreaRoutinesAndContingencyPlan
    } yield for {
      totArea <- mrTotArea
      areaPerimeterSecurity <- mrAreaPerimeterSecurity
      areaTheftProtection <- mrAreaTheftProtection
      areaFireProtection <- mrAreaFireProtection
      areaWaterDamageAssessment <- mrAreaWaterDamageAssessment
      areaRoutinesAndContingencyPlan <- mrAreaRoutinesAndContingencyPlan
    } yield KdReport(
      totalArea = totArea,
      perimeterSecurity = areaPerimeterSecurity,
      theftProtection = areaTheftProtection,
      fireProtection = areaFireProtection,
      waterDamageAssessment = areaWaterDamageAssessment,
      routinesAndContingencyPlan = areaRoutinesAndContingencyPlan
    )
  }

}
