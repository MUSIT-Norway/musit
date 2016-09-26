package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.KdReportDao
import no.uio.musit.microservice.storagefacility.domain.report.KdReport
import no.uio.musit.service.MusitResults.MusitResult

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by ellenjo on 26.09.16.
  */
class KdReportService @Inject()(val kdReportDao: KdReportDao) {
  def getReport : Future[MusitResult[KdReport]] = {
    val futTotArea = kdReportDao.getReportTotalArea
    val futAreaPerimeterSecurity = kdReportDao.getAreaPerimeterSecurity
    val futAreaTheftProtection = kdReportDao.getAreaTheftProtection
    val futAreaFireProtection = kdReportDao.getAreaFireProtectiony
    val futAreaWaterDamageAssessment =  kdReportDao.getAreaWaterDamageAssessment
    val futAreaRoutinesAndContingencyPlan = kdReportDao.getAreaRoutinesAndContingencyPlan
    for {
      mrTotArea <- futTotArea
      mrAreaPerimeterSecurity <- futAreaPerimeterSecurity
      mrAreaTheftProtection <- futAreaTheftProtection
      mrAreaFireProtection <- futAreaFireProtection
      mrAreaWaterDamageAssessment <- futAreaWaterDamageAssessment
      mrAreaRoutinesAndContingencyPlan <- futAreaRoutinesAndContingencyPlan
    } yield
      for {
        totArea <- mrTotArea
        areaPerimeterSecurity <- mrAreaPerimeterSecurity
        areaTheftProtection <- mrAreaTheftProtection
        areaFireProtection <- mrAreaFireProtection
        areaWaterDamageAssessment <- mrAreaWaterDamageAssessment
        areaRoutinesAndContingencyPlan <- mrAreaRoutinesAndContingencyPlan
    } yield
        KdReport(
          totalArea = totArea,
          perimeterSecurity = areaPerimeterSecurity,
          theftProtection = areaTheftProtection,
          fireProtection = areaFireProtection,
          waterDamageAssessment = areaWaterDamageAssessment,
          routinesAndContingencyPlan = areaRoutinesAndContingencyPlan)
    }

}
