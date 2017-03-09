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

package no.uio.musit.service

import org.scalatest.{MustMatchers, WordSpec}

class MusitSearchSpec extends WordSpec with MustMatchers {

  "MusitSearch" should {

    "initialize a new instance of MusitSearch when parsing a search String" in {
      val str = "[a=foo, b=bar, c=baz]"

      val res = MusitSearch.parseSearch(str)

      res.searchMap.size mustBe 3
      res.searchMap.keySet must contain allOf ("a", "b", "c")
      res.searchMap.get("a") mustBe Some("foo")
      res.searchMap.get("b") mustBe Some("bar")
      res.searchMap.get("c") mustBe Some("baz")
    }

    "fail if search string doesn't have valid formatting" in {
      val str = "[n=foo, bar, baz=]"

      an[IllegalArgumentException] must be thrownBy MusitSearch.parseSearch(str)
    }

    "return an empty MusitSearch instance" in {
      MusitSearch.parseSearch("") mustBe MusitSearch.empty
    }

    "tada" in {
      val search = MusitSearch.parseSearch("[baz, foo=bar]")

      search.searchStrings mustBe List("baz")
      search.searchMap mustBe Map("foo" -> "bar")
    }

  }

}
