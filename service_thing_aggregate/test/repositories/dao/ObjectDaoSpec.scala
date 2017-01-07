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

/**
 * NOTE: Test data for these tests are loaded in the evolution scripts in the
 * src/test/resources directory.
 */
class ObjectDaoSpec extends MusitSpecWithAppPerSuite {
  val dao: ObjectDao = fromInstanceCache[ObjectDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val mid = MuseumId(99)

  val allCollections = Seq(MuseumCollection(
    uuid = CollectionUUID(UUID.fromString("925748d6-bf49-4733-afd1-0e127d639f18")),
    name = Some("Arkeologi"),
    oldSchemaNames = Seq(OldDbSchemas.Archeology)
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

  val escapeChar = dao.escapeChar

  "The ObjectDao" when {

    "classifying search criteria" should {

      def wildcard(arg: String, expected: String) = {
        val res = dao.classifyValue(Some(arg))
        res must not be None
        res.get.v mustBe expected
      }

      "replace '%' with the escape character" in {
        wildcard("C*_A", s"C%${escapeChar}_A")
      }

      "replace '*' with '%' and '%' with the escape character" in {
        wildcard("C*%A", s"C%$escapeChar%A")
      }

      "replace '*' with '%' and prefix '_' with the escape character" in {
        wildcard("*_", s"%${escapeChar}_")
      }

      "replace '*' with '%'" in {
        wildcard("C*A", "C%A")
      }

      "not prefix a single'%' with the escape character" in {
        wildcard("%", "%")
      }

      "not prefix a single '_' with the escape character" in {
        wildcard("_", "_")
      }

    }

    "searching for objects" should {

      "find an existing objects searching with museumNo" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C1")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 10

        val res2 = dao.search(
          mid, 1, 10, Some(MuseumNo("C2")), None, None, allCollections
        ).futureValue
        res2.isSuccess mustBe true
        res2.get.matches.length mustBe 1
      }

      "handle paging correctly" in {
        val res1 = dao.search(
          mid, 1, 3, Some(MuseumNo("C1")), None, None, allCollections
        ).futureValue
        res1.isSuccess mustBe true
        val seq1 = res1.get
        seq1.matches.length mustBe 3
        seq1.matches.head.subNo mustBe Some(SubNo("10a"))
        seq1.matches.tail.head.subNo mustBe Some(SubNo("11"))
        seq1.matches.last.subNo mustBe Some(SubNo("12"))

        val res2 = dao.search(
          mid, 2, 3, Some(MuseumNo("C1")), None, None, allCollections
        ).futureValue
        res2.isSuccess mustBe true
        val seq2 = res2.get
        seq2.matches.length mustBe 3
        seq2.matches.head.subNo mustBe Some(SubNo("13"))
        seq2.matches.tail.head.subNo mustBe Some(SubNo("14"))
        seq2.matches.last.subNo mustBe Some(SubNo("15"))

        val res3 = dao.search(
          mid, 3, 3, Some(MuseumNo("C1")), None, None, allCollections
        ).futureValue
        val seq3 = res3.get

        seq3.matches.length mustBe 3
        seq3.matches.head.subNo mustBe Some(SubNo("16"))
        seq3.matches.tail.head.subNo mustBe Some(SubNo("17"))
        seq3.matches.last.subNo mustBe Some(SubNo("1a"))

        seq1.matches must not contain seq2.matches
        seq1.matches must not contain seq3.matches
        seq2.matches must not contain seq3.matches
      }

      "allow search where museumNo has only digits" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("777")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        val seq = res.get

        seq.matches.length mustBe 4
        seq.matches.head.subNo mustBe Some(SubNo("34"))
        seq.matches(1).subNo mustBe Some(SubNo("34A"))
        seq.matches(2).subNo mustBe Some(SubNo("34B"))
        seq.matches(3).subNo mustBe Some(SubNo("35"))
      }

      "allow wildcard search on museumNo" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C555*")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        val seq = res.get

        seq.matches.length mustBe 6
        seq.matches.head.subNo mustBe Some(SubNo("34A"))
        seq.matches(1).subNo mustBe Some(SubNo("34B"))
        seq.matches(2).subNo mustBe Some(SubNo("34C"))
        seq.matches(3).museumNo mustBe MuseumNo("C555A")
        seq.matches(4).museumNo mustBe MuseumNo("C555B")
        seq.matches(5).museumNo mustBe MuseumNo("C555C")
      }

      "return 0 results when attempting SQL-injection" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C.' or 1=1 --")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 0
      }

      "find objects using museumNo, subNo with wildcard and term" in {

        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("c555*")), Some(SubNo("3*")), Some("øks"), allCollections // scalastyle:ignore
        ).futureValue
        res.isSuccess mustBe true
        val seq = res.get

        seq.matches.length mustBe 3
        seq.matches.head.subNo mustBe Some(SubNo("34A"))
        seq.matches(1).subNo mustBe Some(SubNo("34B"))
        seq.matches(2).subNo mustBe Some(SubNo("34C"))
      }

      "find objects using museumNo with wildcard" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("c888_*")), None, Some("øks"), allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 2
        res.get.matches.head.museumNo mustBe MuseumNo("C888_A")
        res.get.matches.last.museumNo mustBe MuseumNo("C888_B")
      }

      "treat '%' like an ordinary character in equality comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C81%A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1 //We should find C81%A and *not* C81%XA
        res.get.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '%' like an ordinary character in like comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C*%A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '-' like an ordinary character in equality comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C81-A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat '-' like an ordinary character in like comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo("C*-A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat the escape character like an ordinary character equality comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo(s"C81${escapeChar}A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

      "treat the escape character like an ordinary character like comparison" in {
        val res = dao.search(
          mid, 1, 10, Some(MuseumNo(s"C*${escapeChar}A")), None, None, allCollections
        ).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

    }

    "getting objects for a nodeId" should {
      "return a list of objects if the nodeId exists in the museum" in {
        val mr = dao.pagedObjects(
          mid = mid,
          nodeId = StorageNodeDatabaseId(4),
          collections = allCollections,
          page = 1,
          limit = 10
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.totalMatches mustBe 3
        mr.get.matches match {
          case Vector(first, second, third) =>
            first.id mustBe ObjectId(1)
            first.museumNo mustBe MuseumNo("C666")
            first.subNo mustBe Some(SubNo("34"))
            first.term mustBe "Øks"
            first.mainObjectId mustBe None

            second.id mustBe ObjectId(2)
            second.museumNo mustBe MuseumNo("C666")
            second.subNo mustBe Some(SubNo("31"))
            second.term mustBe "Sverd"
            second.mainObjectId mustBe None

            third.id mustBe ObjectId(3)
            third.museumNo mustBe MuseumNo("C666")
            third.subNo mustBe Some(SubNo("38"))
            third.term mustBe "Sommerfugl"
            third.mainObjectId mustBe None
        }
      }

      "return a list of objects that includes the main object ID" in {
        val mr = dao.pagedObjects(
          mid = mid,
          nodeId = StorageNodeDatabaseId(7),
          collections = allCollections,
          page = 1,
          limit = 10
        ).futureValue

        mr.isSuccess mustBe true
        mr.get.totalMatches mustBe 3
        mr.get.matches match {
          case Vector(first, second, third) =>
            first.id mustBe ObjectId(48)
            first.museumNo mustBe MuseumNo("K123")
            first.subNo mustBe None
            first.term mustBe "Drakt"
            first.mainObjectId mustBe Some(12)

            second.id mustBe ObjectId(49)
            second.museumNo mustBe MuseumNo("K123")
            second.subNo mustBe None
            second.term mustBe "Skjorte"
            second.mainObjectId mustBe Some(12)

            third.id mustBe ObjectId(50)
            third.museumNo mustBe MuseumNo("K123")
            third.subNo mustBe None
            third.term mustBe "Kjole"
            third.mainObjectId mustBe Some(12)
        }

      }

      "return a an empty list when nodeId doesn't exist in museum" in {
        val mr = dao.pagedObjects(
          mid = mid,
          nodeId = StorageNodeDatabaseId(999999),
          collections = allCollections,
          page = 1,
          limit = 10
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.totalMatches mustBe 0
      }

      "return a an empty vector when museum doesn't exist" in {
        val mr = dao.pagedObjects(
          mid = MuseumId(55),
          nodeId = StorageNodeDatabaseId(2),
          collections = allCollections,
          page = 1,
          limit = 10
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.totalMatches mustBe 0
      }

      "return only the number of objects per page specified in limit" in {
        val mr = dao.pagedObjects(
          mid = mid,
          nodeId = StorageNodeDatabaseId(6),
          collections = allCollections,
          page = 1,
          limit = 10
        ).futureValue
        mr.isSuccess mustBe true
        mr.get.totalMatches mustBe 32
        mr.get.matches.size mustBe 10
      }
    }

    "finding the location of an object using an old objectId and schema" should {
      "return the object" in {
        val res = dao.findByOldId(111L, "USD_ARK_GJENSTAND_O").futureValue
        res.isSuccess mustBe true
        res.get must not be None
        val obj = res.get.get
        obj.id mustBe ObjectId(12L)
        obj.museumId mustBe mid
        obj.term mustBe "Fin øks"
      }

      "return None if not found" in {
        val res = dao.findByOldId(333L, "USD_ARK_GJENSTAND_O").futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

    }

  }
}
