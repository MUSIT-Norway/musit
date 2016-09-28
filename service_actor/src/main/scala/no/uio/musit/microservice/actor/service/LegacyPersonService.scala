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

package no.uio.musit.microservice.actor.service

import com.google.inject.Inject
import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.domain.MusitSearch

import scala.concurrent.Future

class LegacyPersonService @Inject() (val actorDao: ActorDao) {

  def all: Future[Seq[Person]] = {
    actorDao.allPersonsLegacy()
  }

  def find(id: Long): Future[Option[Person]] = {
    actorDao.getPersonLegacyById(id)
  }

  def findDetails(ids: Set[Long]): Future[Seq[Person]] = {
    actorDao.getPersonDetailsByIds(ids)
  }

  def find(search: MusitSearch): Future[Seq[Person]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    actorDao.getPersonLegacyByName(searchString)
  }

  def create(person: Person): Future[Person] = {
    actorDao.insertPersonLegacy(person)
  }

}
