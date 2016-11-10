/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package services

import com.google.inject.Inject
import models.report.KdReport
import no.uio.musit.models.MuseumId
import no.uio.musit.service.MusitResults.MusitResult
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.storage.KdReportDao

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class KdReportService @Inject() (val kdReportDao: KdReportDao) {

  /*
     TODO: This code should be re-written to use a monad transformer to reduce
     the necessity for nesting
   */

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
