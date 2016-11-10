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
import no.uio.musit.security.AuthenticatedUser
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.ActorDao

import scala.concurrent.Future

class UserService @Inject() (val actorDao: ActorDao) {

  /**
   * Gets an actor representing the current user. If it doesn't exist one in the
   * database, it creates one.
   *
   * @param user
   * @return
   */
  def currenUserAsActor(user: AuthenticatedUser): Future[Person] = {
    // TODO: This may be a bit simplistic. we need to check if the retrieved
    // user is known by his/her dataporten user name. If so, we need to _update_
    // the user row instead of adding a new one.
    actorDao.getByDataportenId(user.userInfo.id).flatMap {
      case Some(person) => Future.successful(person)
      case None => actorDao.insertAuthUser(user)
    }
  }
}
