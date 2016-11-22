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

package no.uio.musit.models

import org.scalatest.{MustMatchers, WordSpec}

class MuseumNoSpec extends WordSpec with MustMatchers {

  "Interacting with MuseumNo" should {
    "extract number part when prefixed with a character" in {
      MuseumNo("C4252").asNumber mustBe Some(4252)
    }

    "extract number part when not prefixed or suffixed with characters" in {
      MuseumNo("4252").asNumber mustBe Some(4252)
    }

    "not extract a number if only containing a character" in {
      MuseumNo("C").asNumber mustBe None
    }

    "not extract a number if empty" in {
      MuseumNo("").asNumber mustBe None
    }

    "extract a number if prefix and suffixed with characters" in {
      MuseumNo("C4252a").asNumber mustBe Some(4252)
    }

    "extract a number if prefixed with a character and a ., and has suffix" in {
      MuseumNo("C.4252a").asNumber mustBe Some(4252)
    }

    "extract a number if prefix with character, . and whitespace, and has suffix" in {
      MuseumNo("C. 4252a").asNumber mustBe Some(4252)
    }

    "extract number if prefixed with character and has suffix starting with /" in {
      MuseumNo("B3241/a_7").asNumber mustBe Some(3241)
      MuseumNo("B4610/II_æ").asNumber mustBe Some(4610)
    }

  }
}
