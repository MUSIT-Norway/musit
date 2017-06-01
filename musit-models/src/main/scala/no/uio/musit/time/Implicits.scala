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
