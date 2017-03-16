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
import models.Organisation
import no.uio.musit.models.OrgId
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.service.MusitSearch
import repositories.dao.OrganisationDao

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
class OrganisationService @Inject()(val orgDao: OrganisationDao) {

  def find(id: OrgId): Future[Option[Organisation]] = {
    orgDao.getById(id)
  }

  def find(search: MusitSearch): Future[Seq[Organisation]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    orgDao.getByName(searchString)
  }

  def create(org: Organisation): Future[Organisation] = {
    orgDao.insert(org)
  }

  def update(org: Organisation): Future[MusitResult[Option[Int]]] = {
    orgDao.update(org)
  }

  def remove(id: OrgId): Future[Int] = {
    orgDao.delete(id)
  }

}
