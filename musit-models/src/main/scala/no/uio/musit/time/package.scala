package no.uio.musit

import org.joda.time.{DateTime, DateTimeZone}

package object time {

  /**
   * We should always be explicit about the timezone we work with. Otherwise,
   * we risk ending up using the underlying OS timezone settings, which may
   * vary depending on where the application is running.
   */
  val DefaultTimezone = DateTimeZone.UTC

  def dateTimeNow = DateTime.now(DefaultTimezone)

}
