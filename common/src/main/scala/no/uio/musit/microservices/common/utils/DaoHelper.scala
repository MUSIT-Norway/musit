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

import no.uio.musit.microservices.common.domain.MusitNotFoundException
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by jstabel on 6/6/16.
 */
object DaoHelper {

  def map0ToNotFoundFailure(dbio: DBIO[Int]): DBIO[Int] = {
    dbio.map {
      case 0 => throw new MusitNotFoundException("DAO error, object not found")
      case n => n

    }
  }

  /*Not finished and not used, just the start of an idea...
    def recoverActionWithFilterFailure[T](fut: Future[T], recoverValue: T) = {
      fut.recover {
        case e: NoSuchElementException if e.includes("Action.withFilter failed") => Future.successful(recoverValue)
      }
    }
    */
}
