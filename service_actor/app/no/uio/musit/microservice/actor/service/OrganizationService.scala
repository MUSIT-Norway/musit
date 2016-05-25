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
import no.uio.musit.microservice.actor.domain.Organization
import no.uio.musit.microservices.common.domain.{MusitError, MusitSearch}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Business logic for the Person entity in the microservice, simple lookups and so on.
  */
trait OrganizationService {

  def all = {
    ActorDao.allOrganizations()
  }

  def find(id:Long) = {
    ActorDao.getOrganizationById(id)
  }

  def find(search:MusitSearch) = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    ActorDao.getOrganizationByName(searchString)
  }

  def create(organization:Organization) = {
    ActorDao.insertOrganization(organization)
  }

  def update(organization:Organization): Future[Either[MusitError, Organization]] = {
    ActorDao.updateOrganization(organization).flatMap {
      case 0 => Future.successful(Left(MusitError(400, "Something went wrong with the update")))
      case num => ActorDao.getOrganizationById(organization.id).map {
        case Some(org) => Right(org)
        case None => Left(MusitError(404, "Did not find the object"))
      }
    }
  }

  def remove(id:Long) = {
    ActorDao.deleteOrganization(id)
  }


}
