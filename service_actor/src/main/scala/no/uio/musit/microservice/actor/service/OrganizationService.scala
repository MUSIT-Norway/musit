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
import no.uio.musit.microservice.actor.domain.Organization
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch, MusitStatusMessage }
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Business logic for the Person entity in the microservice, simple lookups and so on.
 */
class OrganizationService @Inject() (val actorDao: ActorDao) {

  def all: Future[Seq[Organization]] = {
    actorDao.allOrganizations()
  }

  def find(id: Long): Future[Option[Organization]] = {
    actorDao.getOrganizationById(id)
  }

  def find(search: MusitSearch): Future[Seq[Organization]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    actorDao.getOrganizationByName(searchString)
  }

  def create(organization: Organization): Future[Organization] = {
    actorDao.insertOrganization(organization)
  }

  def update(organization: Organization): Future[Either[MusitError, MusitStatusMessage]] = {
    actorDao.updateOrganization(organization).map {
      case 0 => Left(MusitError(Status.BAD_REQUEST, "Update did not update any records!"))
      case 1 => Right(MusitStatusMessage("Record was updated!"))
      case _ => Left(MusitError(Status.BAD_REQUEST, "Update updated several records!"))
    }
  }

  def remove(id: Long): Future[Int] = {
    actorDao.deleteOrganization(id)
  }

}
