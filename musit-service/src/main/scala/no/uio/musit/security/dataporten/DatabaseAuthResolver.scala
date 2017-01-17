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
import no.uio.musit.models.{ActorId, Email}
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
        MusitDbError(msg, Option(ex))
    }
  }

  override def saveUserInfo(
    userInfo: UserInfo
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]] = {
    val cmd = usrInfoTable.insertOrUpdate(UserInfo.asTuple(userInfo))

    db.run(cmd).map(_ => MusitSuccess(())).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when saving userinfo for ${userInfo.id}"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  override def userInfo(
    userId: ActorId
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserInfo]]] = {
    db.run(usrInfoTable.filter(_.uuid === userId).result.headOption).map { mt =>
      MusitSuccess(mt.map { t =>
        UserInfo(
          id = t._1,
          secondaryIds = t._2.flatMap(e => Option(Seq(e.value))),
          name = t._3,
          email = t._4,
          picture = t._5
        )
      })
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when getting UserInfo for $userId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }

  }

  override def sessionInit()(
    implicit
    ec: ExecutionContext
  ): Future[MusitResult[SessionUUID]] = {
    logger.debug("Initialize a new UserSession with a generated SessionUUID")
    sessionInit(UserSession.prepare())
  }

  private[dataporten] def sessionInit(
    session: UserSession
  )(implicit ec: ExecutionContext): Future[MusitResult[SessionUUID]] = {
    val cmd = usrSessionTable += session

    db.run(cmd).map(_ => MusitSuccess(session.uuid)).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when initializing a new session in the DB."
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  override def userSession(
    sessionUUID: SessionUUID
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserSession]]] = {
    val query = usrSessionTable.filter(_.uuid === sessionUUID)

    db.run(query.result.headOption).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when fetching session ${sessionUUID.asString}"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  override def updateSession(
    userSession: UserSession
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]] = {
    db.run(usrSessionTable.filter(_.uuid === userSession.uuid).update(userSession)).map {
      case numUpdated: Int if numUpdated == 1 => MusitSuccess(())
      case numUpdated =>
        val msg = s"Unexpected number of rows [$numUpdated] were affected " +
          s"when updating user session ${userSession.uuid}"
        logger.warn(msg)
        MusitDbError(msg)
    }.recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred when updating user session ${userSession.uuid}"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
