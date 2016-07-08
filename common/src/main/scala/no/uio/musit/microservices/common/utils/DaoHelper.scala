/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.{ MusitNotFoundException, MusitTooManyRecordsUpdatedException }
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by jstabel on 6/6/16.
 */
object DaoHelper {

  /*Inserting a single row returns DBIO[Int], while inserting multiple rows returns DBIO[Option[Int]]. The event framework assumes (or may be changed to assume) that 1 means the dto got inserted.
    If a dto has a many-insert as a part, it may want to assume everything went ok (if no exception happens).
    This is sort of a hack, but should work.  (Assuming multi-rows inserts throws an exception, as per the documentation, if not succeeds).*/

  def mapMultiRowInsertResultIntoOk(dbIoOptInt: DBIO[Option[Int]]): DBIO[Int] = dbIoOptInt.map { optInt => optInt.fold(1)(identity) }

  /**
   * The contained int is expected to contain the number of rows updated (in a DBIO). (As is typically returned from a Slick update call).
   * If it is 1, 1 is returned. Else an appropriately failed future us returned.
   */
  def onlyAcceptOneUpdatedRecord(dbio: DBIO[Int]): DBIO[Int] = {
    dbio.map {
      case 0 => throw new MusitNotFoundException("DAO error, object not found")
      case 1 => 1
      case n => throw new MusitTooManyRecordsUpdatedException(s"Update updated too many records! ($n)")
    }
  }
}
