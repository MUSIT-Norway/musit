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

import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch }
import play.api.http.Status

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Business logic for the Person entity in the microservice, simple lookups and so on.
 */
trait PersonService {

  def all: Future[Seq[Person]] = {
    ActorDao.allPersons()
  }

  def find(id: Long): Future[Option[Person]] = {
    ActorDao.getPersonById(id)
  }

  def find(search: MusitSearch): Future[Seq[Person]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    ActorDao.getPersonByName(searchString)
  }

  def create(person: Person): Future[Person] = {
    ActorDao.insertPerson(person)
  }

  def update(person: Person): Future[Either[MusitError, Person]] = {
    ActorDao.updatePerson(person).flatMap {
      case 0 => Future.successful(Left(MusitError(Status.BAD_REQUEST, "Something went wrong with the update")))
      case num => ActorDao.getPersonById(person.id).map {
        case Some(uPerson) => Right(uPerson)
        case None => Left(MusitError(Status.NOT_FOUND, "Did not find the object"))
      }
    }
  }

  def remove(id: Long) = {
    ActorDao.deletePerson(id)
  }

}
