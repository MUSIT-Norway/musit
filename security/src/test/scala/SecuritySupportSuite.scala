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
  * Created by jstabel on 4/4/16.
  */

import org.scalatest.FunSuite
import no.uio.musit.security._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import scala.concurrent.ExecutionContext.Implicits.global

class SecuritySupportSuite extends FunSuite with SecuritySupport {
//{securityContext= new SecurityContext(Seq("Admin", "EtnoSkriv", "EtnoLes"))}
  val groups = List("Admin", "EtnoSkriv", "EtnoLes")


  val securityContext = new no.uio.musit.security.SecurityContext(groups)
  test("should execute if has groups") {
    this.authorize(Seq("Admin")) {
      Future(println("aha in future!"))
    }
      println("Ferdig")
  }


  test("should fail if has deniedGroups") {
   val fut = this.authorize(Seq("Admin"), Seq("EtnoLes")) {
      Future(println("Denne skal ikke synes!!!"))
    }
    intercept[Exception] {
      val answer = Await.result(fut, 2 seconds)

    }
  }


}
