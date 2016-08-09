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

package no.uio.musit.microservice.musitThing.service

import com.google.inject.Inject
import no.uio.musit.microservice.musitThing.dao.MusitThingDao
import no.uio.musit.microservice.musitThing.domain.MusitThing
import play.api.mvc.Request

class MusitThingService @Inject()(musitThingDao: MusitThingDao) {

  def all = musitThingDao.all

  def getById(id: Long) = musitThingDao.getById(id)

  def getDisplayId(id: Long) = musitThingDao.getDisplayId(id)

  def getDisplayName(id: Long) = musitThingDao.getDisplayName(id)

  def create(thing: MusitThing) = musitThingDao.insert(thing)

  def extractFilterFromRequest(request: Request[_]): Array[String] = {
    request.getQueryString("filter") match {
      case Some(filterString) => "^\\[(\\w*)\\]$".r.findFirstIn(filterString) match {
        case Some(str) => str.split(",")
        case None => Array.empty[String]
      }
      case None => Array.empty[String]
    }
  }
}
