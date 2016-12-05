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
import no.uio.musit.security.UserInfo
import no.uio.musit.service.MusitSearch
import repositories.dao.{ActorDao, UserInfoDao}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class ActorService @Inject() (
    val actorDao: ActorDao,
    val usrInfDao: UserInfoDao
) {

  def findByActorId(id: ActorId): Future[Option[Person]] = {
    val userInfoPerson = usrInfDao.getById(id)
    val actor = actorDao.getByActorId(id)

    for {
      uip <- userInfoPerson
      act <- actor
    } yield {
      // We prefer the result from UserInfo over the one in Actor if the
      // ID is registered as an active user of the system.
      uip.map(Person.fromUserInfo).orElse(act)
    }
  }

  def findDetails(ids: Set[ActorId]): Future[Seq[Person]] = {
    val users = usrInfDao.listBy(ids)
    val actors = actorDao.listBy(ids)

    for {
      u <- users
      a <- actors
    } yield dedupe(u, a)
  }

  def findByName(search: MusitSearch): Future[Seq[Person]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    val users = usrInfDao.getByName(searchString)
    val actors = actorDao.getByName(searchString)

    for {
      u <- users
      a <- actors
    } yield dedupe(u, a)
  }

  private def dedupe(users: Seq[UserInfo], actors: Seq[Person]): Seq[Person] = {
    actors.filterNot { p =>
      users.exists { u =>
        p.dataportenUser.exists(prefix => u.feideUser.exists(_.value.startsWith(prefix)))
      }
    }.union(users.map(Person.fromUserInfo))
  }

}
