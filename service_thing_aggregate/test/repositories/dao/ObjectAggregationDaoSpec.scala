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

import java.util.UUID

import no.uio.musit.models._
import no.uio.musit.security.{AuthenticatedUser, GroupInfo, Permissions, UserInfo}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class ObjectAggregationDaoSpec extends MusitSpecWithAppPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val dao: ObjectAggregationDao = fromInstanceCache[ObjectAggregationDao]

  val mid = MuseumId(99)

  val allCollections = Seq(MuseumCollection(
    uuid = CollectionUUID(UUID.fromString("925748d6-bf49-4733-afd1-0e127d639f18")),
    name = Some("AllCollections"),
    oldSchemaNames = OldDbSchemas.all
  ))

  implicit val dummyUser = AuthenticatedUser(
    userInfo = UserInfo(
      id = ActorId.generate(),
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(GroupInfo(
      id = GroupId.generate(),
      name = "FooBarGroup",
      permission = Permissions.Admin,
      museumId = mid,
      description = None,
      collections = allCollections
    ))
  )

  "Interacting with the ObjectAggregationDao" when {

    "getting objects for a nodeId that exists within a museum" should {
      "return a list of objects" in {
        val mr = dao.getObjects(
          mid,
          StorageNodeDatabaseId(3),
          allCollections
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.size mustBe 3
        mr.get match {
          case Vector(first, second, third) =>
            first.id mustBe ObjectId(1)
            first.museumNo mustBe MuseumNo("C666")
            first.subNo mustBe Some(SubNo("34"))
            first.term mustBe Some("Øks")

            second.id mustBe ObjectId(2)
            second.museumNo mustBe MuseumNo("C666")
            second.subNo mustBe Some(SubNo("31"))
            second.term mustBe Some("Sverd")

            third.id mustBe ObjectId(3)
            third.museumNo mustBe MuseumNo("C666")
            third.subNo mustBe Some(SubNo("38"))
            third.term mustBe Some("Sommerfugl")
        }
      }
    }

    "get objects for a nodeId that does not exist in museum" should {
      "return a an empty vector" in {
        val mr = dao.getObjects(
          mid,
          StorageNodeDatabaseId(999999),
          allCollections
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.length mustBe 0
      }
    }
    "get objects for a museum that does not exist" should {
      "return a an empty vector" in {
        val mr = dao.getObjects(
          MuseumId(55),
          StorageNodeDatabaseId(2),
          allCollections
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.length mustBe 0
      }
    }
  }
}
