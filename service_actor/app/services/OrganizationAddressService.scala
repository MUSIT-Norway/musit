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
import models.OrganizationAddress
import no.uio.musit.service.MusitResults.MusitResult
import repositories.dao.ActorDao

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

  def update(address: OrganizationAddress): Future[MusitResult[Option[Int]]] = {
    actorDao.updateOrganizationAddress(address)
  }

  def remove(id: Long): Future[Int] = {
    actorDao.deleteOrganizationAddress(id)
  }

}
