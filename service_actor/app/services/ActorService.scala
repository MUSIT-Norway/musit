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
import models.Person
import no.uio.musit.models.ActorId
import no.uio.musit.service.MusitSearch
import repositories.dao.ActorDao

import scala.concurrent.Future

class ActorService @Inject() (val actorDao: ActorDao) {

  def find(id: ActorId): Future[Option[Person]] = {
    actorDao.getById(id)
  }

  def findDetails(ids: Set[ActorId]): Future[Seq[Person]] = {
    actorDao.listByIds(ids)
  }

  def find(search: MusitSearch): Future[Seq[Person]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    actorDao.getByName(searchString)
  }

  def create(person: Person): Future[Person] = {
    actorDao.insert(person)
  }

}
