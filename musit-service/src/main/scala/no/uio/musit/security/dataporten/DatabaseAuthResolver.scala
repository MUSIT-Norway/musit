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

package no.uio.musit.security.dataporten

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.Email
import no.uio.musit.security._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class DatabaseAuthResolver @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends AuthTables with AuthResolver {

  import driver.api._

  val logger = Logger(classOf[DatabaseAuthResolver])

  override def findGroupInfoByFeideEmail(
    feideEmail: Email
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[GroupInfo]]] = {
    findGroupInfoBy(usrGrpTable.filter(_.feideEmail === feideEmail)).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when trying to find Groups for user $feideEmail"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  override def saveUserInfo(
    userInfo: UserInfo
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]] = {
    val cmd = usrInfoTable.insertOrUpdate(UserInfo.asTuple(userInfo))

    db.run(cmd).map(_ => MusitSuccess(())).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when upserting userinfo for ${userInfo.id}"
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

}
