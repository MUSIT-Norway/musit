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

package no.uio.musit.security


import scala.concurrent.Future
import no.uio.musit.microservices.common.extensions.SeqExtensions._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by jstabel on 4/1/16.
  * authorize
  * requires( Optional[List"Admin"] = None, Optional = None
  * withGroups(Optional["Admin"], Optional["Guest"])(withoutGroups(["Guest"])( ctx => {}))
  */
/*#OLD
trait OldSecuritySupport {

  val securityContext: SecurityContext = new SecurityContext()

  def initWithToken(token: String): Unit = {


  }

  def initWithGroups(userGroups: Seq[String]): Unit = {
    securityContext.userGroups = userGroups
  }


  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => Future[T]): Future[T] = {
    if (securityContext.hasAllGroups(requiredGroups) && securityContext.hasNoneOfGroups(deniedGroups)) {
      body
    }
    else {
      Future.failed(new Exception("Unauthorized"))
    }
  }

  def foo = {
    authorize(Seq("Admin")) {
      Future(println("wow"))
    }
  }
}
*/