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

import no.uio.musit.models.{MuseumNo, SubNo}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

/**
 * NOTE: Test data for these tests are loaded in the evolution scripts in the
 * src/test/resources directory.
 */
class ObjectSearchDaoSpec extends MusitSpecWithAppPerSuite {
  val dao: ObjectSearchDao = fromInstanceCache[ObjectSearchDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val escapeChar = dao.escapeChar

  "The ObjectSearchDao" when {

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
        val res = dao.search(2, 1, 10, Some(MuseumNo("C1")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 10

        val res2 = dao.search(2, 1, 10, Some(MuseumNo("C2")), None, None).futureValue
        res2.isSuccess mustBe true
        res2.get.matches.length mustBe 1
      }

      "handle paging correctly" in {
        val res1 = dao.search(2, 1, 3, Some(MuseumNo("C1")), None, None).futureValue
        res1.isSuccess mustBe true
        val seq1 = res1.get
        seq1.matches.length mustBe 3
        seq1.matches.head.subNo mustBe Some(SubNo("10a"))
        seq1.matches.tail.head.subNo mustBe Some(SubNo("11"))
        seq1.matches.last.subNo mustBe Some(SubNo("12"))

        val res2 = dao.search(2, 2, 3, Some(MuseumNo("C1")), None, None).futureValue
        res2.isSuccess mustBe true
        val seq2 = res2.get
        seq2.matches.length mustBe 3
        seq2.matches.head.subNo mustBe Some(SubNo("13"))
        seq2.matches.tail.head.subNo mustBe Some(SubNo("14"))
        seq2.matches.last.subNo mustBe Some(SubNo("15"))

        val res3 = dao.search(2, 3, 3, Some(MuseumNo("C1")), None, None).futureValue
        val seq3 = res3.get

        seq3.matches.length mustBe 3
        seq3.matches.head.subNo mustBe Some(SubNo("16"))
        seq3.matches.tail.head.subNo mustBe Some(SubNo("17"))
        seq3.matches.last.subNo mustBe Some(SubNo("18"))

        seq1.matches must not contain seq2.matches
        seq1.matches must not contain seq3.matches
        seq2.matches must not contain seq3.matches
      }

      "allow search where museumNo has only digits" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("777")), None, None).futureValue
        res.isSuccess mustBe true
        val seq = res.get

        seq.matches.length mustBe 4
        seq.matches.head.subNo mustBe Some(SubNo("34"))
        seq.matches(1).subNo mustBe Some(SubNo("34A"))
        seq.matches(2).subNo mustBe Some(SubNo("34B"))
        seq.matches(3).subNo mustBe Some(SubNo("35"))
      }

      "allow wildcard search on museumNo" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("C555*")), None, None).futureValue
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
        val res = dao.search(2, 1, 10, Some(MuseumNo("C.' or 1=1 --")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 0
      }

      "find objects using museumNo, subNo with wildcard and term" in {

        val res = dao.search(2, 1, 10, Some(MuseumNo("c555*")), Some(SubNo("3*")), Some("øks")).futureValue
        res.isSuccess mustBe true
        val seq = res.get

        seq.matches.length mustBe 3
        seq.matches.head.subNo mustBe Some(SubNo("34A"))
        seq.matches(1).subNo mustBe Some(SubNo("34B"))
        seq.matches(2).subNo mustBe Some(SubNo("34C"))
      }

      "find objects using museumNo with wildcard" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("c888_*")), None, Some("øks")).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 2
        res.get.matches.head.museumNo mustBe MuseumNo("C888_A")
        res.get.matches.last.museumNo mustBe MuseumNo("C888_B")
      }

      "treat '%' like an ordinary character in equality comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("C81%A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1 //We should find C81%A and *not* C81%XA
        res.get.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '%' like an ordinary character in like comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("C*%A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81%A")
      }

      "treat '-' like an ordinary character in equality comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("C81-A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat '-' like an ordinary character in like comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo("C*-A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo("C81-A")
      }

      "treat the escape character like an ordinary character equality comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo(s"C81${escapeChar}A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

      "treat the escape character like an ordinary character like comparison" in {
        val res = dao.search(2, 1, 10, Some(MuseumNo(s"C*${escapeChar}A")), None, None).futureValue
        res.isSuccess mustBe true
        res.get.matches.length mustBe 1
        res.get.matches.head.museumNo mustBe MuseumNo(s"C81${escapeChar}A")
      }

    }
  }
}
