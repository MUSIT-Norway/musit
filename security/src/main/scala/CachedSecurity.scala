import no.uio.musit.security.{UserInfo, UserInfoProvider}

import scala.concurrent.Future

/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

/**
  * Created by jstabel on 4/27/16.
  */

/*
trait UserInfoProvider {
  def getUserInfo(userid: String) : Future[Option[UserInfo]]
  def getUserGroups(userid: String): Future[Option[Seq[String]]]
}
*/
object Constants
{
  val securityPrefix = "Security"
}

/*
class CachedSecurity extends UserInfoProvider {


  def getUserInfo(userid: String) : Future[Option[UserInfo]]
  def getUserGroups(userid: String): Future[Option[Seq[String]]]

}
*/