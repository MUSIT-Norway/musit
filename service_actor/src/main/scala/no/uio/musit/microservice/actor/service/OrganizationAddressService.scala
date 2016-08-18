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
import no.uio.musit.microservice.actor.domain.OrganizationAddress
import no.uio.musit.microservices.common.domain.{ MusitError, MusitStatusMessage }
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Business logic for the Person entity in the microservice, simple lookups and so on.
 */
class OrganizationAddressService @Inject() (val actorDao: ActorDao) {

  def all(organizationId: Long): Future[Seq[OrganizationAddress]] = {
    actorDao.allAddressesForOrganization(organizationId)
  }

  def find(id: Long): Future[Option[OrganizationAddress]] = {
    actorDao.getOrganizationAddressById(id)
  }

  def create(address: OrganizationAddress): Future[OrganizationAddress] = {
    actorDao.insertOrganizationAddress(address)
  }

  def update(address: OrganizationAddress): Future[Either[MusitError, MusitStatusMessage]] = {
    actorDao.updateOrganizationAddress(address).map {
      case 0 => Left(MusitError(Status.BAD_REQUEST, "No records were updated!"))
      case 1 => Right(MusitStatusMessage("Record was updated!"))
      case _ => Left(MusitError(Status.BAD_REQUEST, "Several records were updated!"))
    }
  }

  def remove(id: Long): Future[Int] = {
    actorDao.deleteOrganizationAddress(id)
  }

}
