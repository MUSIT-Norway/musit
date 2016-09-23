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

package no.uio.musit.test

import org.scalatest.TestData
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, OneAppPerTest, PlaySpec }
import play.api.Application

trait MusitSpec extends PlaySpec with ScalaFutures

trait MusitSpecWithApp extends MusitSpec with MusitFakeApplication {
  val dbName: String
  // NOTE: This is mutable because of the usage in specs that require a new
  // application per test.
  var musitFakeApp = createApplication(dbName)

  def fromInstanceCache[T](implicit manifest: Manifest[T]): T = {
    val instance = Application.instanceCache[T]
    instance(musitFakeApp)
  }

}

trait MusitSpecWithAppPerTest extends MusitSpecWithApp with OneAppPerTest {
  implicit override def newAppForTest(testData: TestData): Application = {
    musitFakeApp = createApplication(dbName)
    musitFakeApp
  }

}

trait MusitSpecWithAppPerSuite extends MusitSpecWithApp with OneAppPerSuite {
  implicit override lazy val app = musitFakeApp
}

// TODO: Add traits for fake server per app and suite.
