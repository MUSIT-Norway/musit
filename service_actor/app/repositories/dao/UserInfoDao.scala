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

package repositories.dao

import com.google.inject.Inject
import no.uio.musit.models.ActorId
import no.uio.musit.security.{AuthTables, UserInfo}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

class UserInfoDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with AuthTables {

  val logger = Logger(classOf[UserInfoDao])

  import driver.api._

  private def userInfoFromTuple(tuple: UserInfoTableType): UserInfo = {
    UserInfo(
      id = tuple._1,
      secondaryIds = tuple._2.map(sec => Seq(sec)),
      name = tuple._3,
      email = tuple._4,
      picture = tuple._5
    )
  }

  def getById(id: ActorId): Future[Option[UserInfo]] = {
    db.run(usrInfoTable.filter(_.uuid === id).result.headOption).map { musr =>
      musr.map(userInfoFromTuple)
    }.recover {
      case NonFatal(ex) =>
        logger.error(s"An error occurred reading UserInfo for $id", ex)
        None
    }
  }

  def listBy(ids: Set[ActorId]): Future[Seq[UserInfo]] = {
    val query = usrInfoTable.filter(_.uuid inSet ids).sortBy(_.name)
    db.run(query.result.map(_.map(userInfoFromTuple)))
  }.recover {
    case NonFatal(ex) =>
      logger.error(s"An error occurred reading UserInfo for ${ids.mkString(", ")}", ex)
      Seq.empty
  }

  def getByName(searchString: String): Future[Seq[UserInfo]] = {
    val likeArg = searchString.toUpperCase
    val query = usrInfoTable.filter(_.name.toUpperCase like s"%$likeArg%").sortBy(_.name)
    db.run(query.result.map(_.map(userInfoFromTuple))).recover {
      case NonFatal(ex) =>
        logger.error(s"An error occurred searching for UserInfo with $searchString", ex)
        Seq.empty
    }
  }

}
