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

package models.datetime

import java.time.{ZoneId, ZoneOffset}

import org.scalatest.{MustMatchers, WordSpec}
import Implicits._

class DateTimeImplicitsSpec extends WordSpec with MustMatchers {

  "Converting between DateTime and java.sql.Timezone" should {
    "result in correct values" in {

      val dt1 = dateTimeNow
      val ts = dateTimeToJTimestamp(dt1)

      val zdt = ts.toInstant.atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(1)))

      val dt2 = jSqlTimestampToDateTime(ts)

      dt2 mustBe dt1
      zdt.getDayOfYear mustBe dt1.getDayOfYear
      zdt.getHour mustBe dt1.hourOfDay().get() + 1
    }
  }

}
