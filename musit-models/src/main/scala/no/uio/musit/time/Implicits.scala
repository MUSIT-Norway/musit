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

package no.uio.musit.time

import java.sql.{Date => JSqlDate, Timestamp => JSqlTimestamp}

import org.joda.time.DateTime

trait DateTimeImplicits {

  implicit def dateTimeToJSqlDate(dt: DateTime): JSqlDate =
    new JSqlDate(dt.getMillis)

  implicit def dateTimeToJTimestamp(dt: DateTime): JSqlTimestamp =
    new JSqlTimestamp(dt.getMillis)

  implicit def jSqlDateToDateTime(jsd: JSqlDate): DateTime =
    new DateTime(jsd, DefaultTimezone)

  implicit def jSqlTimestampToDateTime(jst: JSqlTimestamp): DateTime =
    new DateTime(jst, DefaultTimezone)

  implicit def optDateTimeToJSqlDate(
    mdt: Option[DateTime]
  ): Option[JSqlDate] = mdt.map(dateTimeToJSqlDate)

  implicit def optDateTimeToJSqlTimestamp(
    mdt: Option[DateTime]
  ): Option[JSqlTimestamp] = mdt.map(dateTimeToJTimestamp)

  implicit def optJSqlDateToDateTime(
    mjsd: Option[JSqlDate]
  ): Option[DateTime] = mjsd.map(jSqlDateToDateTime)

  implicit def optJSqlTimestampToDateTime(
    mjst: Option[JSqlTimestamp]
  ): Option[DateTime] = mjst.map(jSqlTimestampToDateTime)

}

object Implicits extends DateTimeImplicits
