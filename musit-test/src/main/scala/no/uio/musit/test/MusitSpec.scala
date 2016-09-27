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

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.Application
import play.api.test.TestServer

/**
 * Base trait to use
 */
trait MusitSpec extends PlaySpec with ScalaFutures

/**
 * Mixin this trait if all you need is a fake Play application.
 */
trait MusitSpecWithApp extends MusitSpec with MusitFakeApplication {
  // ¡¡¡NOTE: Do not make this immutable!!!
  var musitFakeApp = createApplication()

  def fromInstanceCache[T](implicit manifest: Manifest[T]): T = {
    val instance = Application.instanceCache[T]
    instance(musitFakeApp)
  }

}

/**
 * Mixin this trait if you need a fresh fake Play application per test in a
 * *Spec.scala file.
 */
trait MusitSpecWithAppPerTest extends MusitSpecWithApp with OneAppPerTest {
  implicit override def newAppForTest(testData: TestData): Application = {
    musitFakeApp = createApplication()
    musitFakeApp
  }

}

/**
 * Mixin this trait if you need a fresh fake Play application for all tests in a
 * *Spec.scala file. This is also the one to use if you
 * bundle tests into a bigger Suite involving more than 1 file.
 */
trait MusitSpecWithAppPerSuite extends MusitSpecWithApp with OneAppPerSuite {
  implicit override lazy val app = musitFakeApp
}

/**
 * For integration testing you will need to mixin a running server with a fake
 * application. Use this trait to have 1 server start/stop per test in a
 * *Spec.scala file.
 */
trait MusitSpecWithServerPerTest extends MusitSpecWithApp
    with Network
    with OneServerPerTest {

  override lazy val port: Int = generatePort

  implicit override def newAppForTest(testData: TestData): Application = {
    musitFakeApp = createApplication()
    musitFakeApp
  }
}

/**
 * For integration testing you will need to mixin a running server with a fake
 * application. Use this trait to have 1 server start/stop per  *Spec.scala
 * file. This is also to be used when bundling specs in a bigger Suite of tests.
 */
trait MusitSpecWithServerPerSuite extends MusitSpecWithApp
    with Network
    with SuiteMixin
    with ServerProvider { this: Suite =>

  override lazy val port: Int = generatePort

  implicit override lazy val app = musitFakeApp

  def beforeTests(): Unit = ()

  def afterTests(): Unit = ()

  /**
   * Overriding the default run method in OneServerPerSuite to be able to pre-
   * load test data for the test-scenario.
   */
  override def run(testName: Option[String], args: Args): Status = {
    val testServer = TestServer(port, app)
    testServer.start()

    beforeTests()

    try {
      val newConfigMap = args.configMap +
        ("org.scalatestplus.play.app" -> app) +
        ("org.scalatestplus.play.port" -> port)

      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted { _ =>
        afterTests()
        testServer.stop()
      }
      status
    } catch { // In case the suite aborts, ensure the server is stopped
      case ex: Throwable =>
        afterTests()
        testServer.stop()
        throw ex // scalastyle:ignore
    }
  }

}
