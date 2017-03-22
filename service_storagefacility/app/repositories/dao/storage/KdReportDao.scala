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

package repositories.dao.storage

import com.google.inject.Inject
import models.storage.StorageType
import models.storage.StorageType.RoomType
import no.uio.musit.models.MuseumId
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.StorageTables

import scala.concurrent.Future
import scala.util.control.NonFatal

class KdReportDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends StorageTables {

  import profile.api._

  private val roomType: StorageType = RoomType

  def getReportTotalArea(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = storageNodeTable.filter { sn =>
      sn.storageType === roomType && sn.museumId === mid && sn.isDeleted === false
    }.map(_.area)

    db.run(query.sum.result).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError("Error occurred while retrieving objects", Some(e))
    }
  }

  def getAreaPerimeterSecurity(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.perimeterSecurity === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError("Error occurred while retrieving areaPerimeterSecurity", Some(e))
    }
  }

  def getAreaTheftProtection(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.theftProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError("Error occurred while retrieving AreaTheftProtection", Some(e))
    }
  }

  def getAreaFireProtectiony(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.fireProtection === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError("Error occurred while retrieving AreaFireProtection", Some(e))
    }
  }

  def getAreaWaterDamageAssessment(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.waterDamage === true)
    } yield {
      sn.area
    }
    val action = query.sum.result
    db.run(action).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError(
          message = "Error occurred while retrieving areaWaterDamageAssessment",
          ex = Some(e)
        )
    }
  }

  def getAreaRoutinesAndContingencyPlan(mid: MuseumId): Future[MusitResult[Double]] = {
    val query = for {
      sn <- storageNodeTable.filter(sn => sn.museumId === mid && sn.isDeleted === false)
      r  <- roomTable.filter(r => sn.id === r.id && r.routinesAndContingency === true)
    } yield {
      sn.area
    }
    db.run(query.sum.result).map(res => MusitSuccess(res.getOrElse(0.0))).recover {
      case NonFatal(e) =>
        MusitDbError(
          message = "Error occurred while retrieving AreaRoutinesAndContingencyPlan",
          ex = Some(e)
        )
    }
  }

}
