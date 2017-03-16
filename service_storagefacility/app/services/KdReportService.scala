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
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.MonadTransformers.MusitResultT
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.storage.KdReportDao
import no.uio.musit.functional.Implicits._

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class KdReportService @Inject()(val dao: KdReportDao) {

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
