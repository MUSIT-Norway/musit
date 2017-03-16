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
import models.OrganisationAddress
import no.uio.musit.models.{DatabaseId, OrgId}
import no.uio.musit.MusitResults.MusitResult
import repositories.dao.AddressDao

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class OrganisationAddressService @Inject()(val adrDao: AddressDao) {

  def all(organizationId: OrgId): Future[Seq[OrganisationAddress]] = {
    adrDao.allFor(organizationId)
  }

  def find(id: DatabaseId): Future[Option[OrganisationAddress]] = {
    adrDao.getById(id)
  }

  def create(address: OrganisationAddress): Future[OrganisationAddress] = {
    adrDao.insert(address)
  }

  def update(address: OrganisationAddress): Future[MusitResult[Option[Int]]] = {
    adrDao.update(address)
  }

  def remove(id: DatabaseId): Future[Int] = {
    adrDao.delete(id)
  }

}
