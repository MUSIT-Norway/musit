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

package no.uio.musit.microservices.common.linking.dao

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import org.scalatest._
import play.api.Logger
import play.api.test.{FakeApplication, TestServer}

case class MockTable(id:Long, links:Seq[Link]) extends BaseMusitDomain

class LinkDaoSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val host = "http://localhost:7070"

  def app_ = {
    FakeApplication(additionalConfiguration =
      Map(
        "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:play-test",
        "evolutionplugin" -> "enabled"
      ))
  }

  def server_ = TestServer(application = app_, port = 7070)

  override protected def beforeAll(): Unit = {
    server_.start

  }

  override protected def afterAll(): Unit = {
    server_.stop
  }

  /* Unit tester */
  "DatabaseConfig" should "work" in {
    import LinkDao._
    insert(MockTable(1, Seq.empty[Link]), "test", "/test/case/100")
    var allLinks = findAllLinks
    allLinks.map(_.foreach( (link:Link) =>
        Logger.info(s"test: $link")
      )
    )
  }



}
