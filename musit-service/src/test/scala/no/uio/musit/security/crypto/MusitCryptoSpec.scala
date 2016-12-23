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

package no.uio.musit.security.crypto

import no.uio.musit.test.MusitSpecWithAppPerSuite

class MusitCryptoSpec extends MusitSpecWithAppPerSuite {

  val crypto = fromInstanceCache[MusitCrypto]

  "MusitCrypto" should {

    "successfully encrypt a token" in {
      val orig = "27b9c7bc-06c3-4cc5-8c83-b34125377dd6"
      val res = crypto.encryptAES(orig)
      res must not be orig
      res must not include " "
    }

    "successfully decrypt an encrypted token" in {
      val orig = "27b9c7bc-06c3-4cc5-8c83-b34125377dd6"
      val enc = crypto.encryptAES(orig)
      val res = crypto.decryptAES(enc)

      res mustBe orig
    }
  }

}
