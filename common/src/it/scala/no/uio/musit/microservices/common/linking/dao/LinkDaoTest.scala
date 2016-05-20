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

import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Logger
import play.api.inject.guice.GuiceApplicationBuilder

case class MockTable(id: Long, links: Seq[Link]) extends BaseMusitDomain

class LinkDaoTest extends PlaySpec with OneAppPerSuite with ScalaFutures {

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "LinkDao test" must {
    import LinkDao._

    "dao should be able to insert and select from table" in {
      insert(MockTable(1, Seq.empty[Link]), "test", "/test/case/100")
      val allLinks = findAllLinks()
      whenReady(allLinks, PlayTestDefaults.timeout) { links =>
        assert(links.length == 1)
        links.foreach(link =>
          Logger.info(s"test: $link"))
      }
    }
  }

}
