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

package dao

import no.uio.musit.service.MusitResults.MusitSuccess
import models.{ MuseumIdentifier, ObjectId }
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{ Millis, Seconds, Span }
import play.api.Logger

class ObjectAggregationDaoSpec extends MusitSpecWithAppPerSuite {

  override val dbName: String = "obj-agr-dao-spec"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val dao: ObjectAggregationDao = fromInstanceCache[ObjectAggregationDao]

  "Interacting with the ObjectAggregationDao" when {

    "getting objects for a nodeId that exists" should {
      "return a list of objects" in {
        val mr = dao.getObjects(3)
        val fut = mr.futureValue
        fut match {
          case MusitSuccess(result) =>
            result match {
              case Vector(first, second, third) =>
                first.id mustBe ObjectId(1)
                first.identifier mustBe MuseumIdentifier("C666", Some("34"))
                first.displayName mustBe Some("Øks")

                second.id mustBe ObjectId(2)
                second.identifier mustBe MuseumIdentifier("C666", Some("31"))
                second.displayName mustBe Some("Sverd")

                third.id mustBe ObjectId(3)
                third.identifier mustBe MuseumIdentifier("C666", Some("38"))
                third.displayName mustBe Some("Sommerfugl")
            }
          case _ =>
            Logger.error("something went wrong")
            fail("This went TOTALLY off the road")
        }
      }
    }

    "getting objects for a nodeId that does not exist" should {
      "return a an empty vector" in {
        val mr = dao.getObjects(999999)
        val fut = mr.futureValue
        fut match {
          case MusitSuccess(result) =>
            result.length mustBe 0
          case _ => fail("Should fail")
        }
      }
    }
  }
}

